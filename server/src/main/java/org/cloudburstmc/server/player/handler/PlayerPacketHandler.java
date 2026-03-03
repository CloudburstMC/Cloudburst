package org.cloudburstmc.server.player.handler;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.google.inject.Inject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.ItemFrame;
import org.cloudburstmc.api.blockentity.Lectern;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.misc.ExperienceOrb;
import org.cloudburstmc.api.event.block.ItemFrameDropItemEvent;
import org.cloudburstmc.api.event.block.LecternPageChangeEvent;
import org.cloudburstmc.api.event.player.*;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.MapItem;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.chunk.LockableChunk;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.registry.GlobalRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.data.*;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryActionData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventorySource;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.container.screen.CloudPlayerInventoryScreen;
import org.cloudburstmc.server.entity.projectile.EntityArrow;
import org.cloudburstmc.server.entity.vehicle.EntityAbstractMinecart;
import org.cloudburstmc.server.entity.vehicle.EntityBoat;
import org.cloudburstmc.server.event.server.DataPacketReceiveEvent;
import org.cloudburstmc.server.form.CustomForm;
import org.cloudburstmc.server.form.Form;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.particle.PunchBlockParticle;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.utils.TextFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.cloudburstmc.server.player.CloudPlayer.DEFAULT_SPEED;

/**
 * Handles all incoming packets from a connected player.
 */
@Log4j2
public class PlayerPacketHandler implements BedrockPacketHandler {
    private final CloudPlayer player;

    protected Vector3i lastRightClickPos = null;
    protected double lastRightClickTime = 0.0;
    @Inject
    GlobalRegistry globalRegistry;
    private Vector3i lastBreakPosition = Vector3i.ZERO;

    public PlayerPacketHandler(CloudPlayer player) {
        this.player = player;
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        if (!player.isConnected()) {
            return PacketSignal.HANDLED;
        }

        try (Timing ignored = Timings.getReceiveDataPacketTiming(packet).startTiming()) {
            if (log.isTraceEnabled() && !player.getServer().isIgnoredPacket(packet.getClass())) {
                log.trace("Inbound {}: {}", player.getName(), packet);
            }

            DataPacketReceiveEvent receiveEvent = new DataPacketReceiveEvent(player, packet);
            player.getServer().getEventManager().fire(receiveEvent);
            if (receiveEvent.isCancelled()) {
                return PacketSignal.HANDLED;
            }

            return packet.handle(this);
        }
    }

    @Override
    public PacketSignal handle(PlayerSkinPacket packet) {
        SerializedSkin skin = packet.getSkin();


        if (!skin.isValid()) {
            return PacketSignal.HANDLED;
        }

        PlayerChangeSkinEvent playerChangeSkinEvent = new PlayerChangeSkinEvent(player, player.getLoginChainData().getSkin());
        playerChangeSkinEvent.setCancelled(TimeUnit.SECONDS.toMillis(player.getServer().getPlayerSkinChangeCooldown()) > System.currentTimeMillis() - player.lastSkinChange);
        player.getServer().getEventManager().fire(playerChangeSkinEvent);
        if (!playerChangeSkinEvent.isCancelled()) {
            player.lastSkinChange = System.currentTimeMillis();
            player.setSkin(skin);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(PlayerInputPacket packet) {
        if (!player.isAlive() || !player.spawned) {
            return PacketSignal.HANDLED;
        }
        if (player.getVehicle() instanceof EntityAbstractMinecart) {
            ((EntityAbstractMinecart) player.getVehicle()).setCurrentSpeed(packet.getInputMotion().getY());
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(MovePlayerPacket packet) {
        log.debug("Received unexpected MovePlayerPacket from {}", player.getName());
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(PlayerAuthInputPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return PacketSignal.HANDLED;
        }

        Set<PlayerAuthInputData> inputData = packet.getInputData();

        processMovement(packet);

        if (inputData.contains(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS)) {
            processBlockActions(packet);
        }

        if (inputData.contains(PlayerAuthInputData.PERFORM_ITEM_INTERACTION)) {
            processItemUseTransaction(packet);
        }

        if (inputData.contains(PlayerAuthInputData.PERFORM_ITEM_STACK_REQUEST) && packet.getItemStackRequest() != null) {
            player.getItemStackNetManager().handleSingleRequest(packet.getItemStackRequest());
        }

        processInputFlags(inputData);

        return PacketSignal.HANDLED;
    }

    private void processMovement(PlayerAuthInputPacket packet) {
        if (player.getTeleportPosition() != null) {
            return;
        }

        Vector3f newPos = packet.getPosition().sub(0, player.getEyeHeight(), 0);
        Vector3f currentPos = player.getPosition();

        float yaw = packet.getRotation().getY() % 360;
        float pitch = packet.getRotation().getX() % 360;

        if (yaw < 0) {
            yaw += 360;
        }

        if (newPos.distanceSquared(currentPos) < 0.01 && yaw == player.getYaw() && pitch == player.getPitch()) {
            return;
        }

        if (currentPos.distance(newPos) > 50) {
            log.debug("PlayerAuthInput packet too far, REVERTING");
            player.sendPosition(currentPos, yaw, pitch, MovePlayerPacket.Mode.RESPAWN);
            return;
        }

        boolean revert = false;
        if (!player.isAlive() || !player.spawned) {
            revert = true;
            player.setForceMovement(currentPos);
        }

        if (player.getForceMovement() != null && (newPos.distanceSquared(player.getForceMovement()) > 0.1 || revert)) {
            log.debug("PlayerAuthInput forceMovement {} REVERTING {}", player.getForceMovement(), newPos);
            player.sendPosition(player.getForceMovement(), yaw, pitch, MovePlayerPacket.Mode.RESPAWN);
        } else {
            player.setRotation(yaw, pitch);
            player.setNewPosition(newPos);
            player.setForceMovement(null);
        }

        if (player.getVehicle() != null) {
            if (player.getVehicle() instanceof EntityBoat) {
                player.getVehicle().setPositionAndRotation(newPos.sub(0, 1, 0), (yaw + 90) % 360, 0);
            }
        }
    }

    private void processBlockActions(PlayerAuthInputPacket packet) {
        for (PlayerBlockActionData actionData : packet.getPlayerActions()) {
            Vector3i blockPos = actionData.getBlockPosition();
            Direction face = Direction.fromIndex(actionData.getFace());

            switch (actionData.getAction()) {
                case START_BREAK:
                    handleStartBreak(blockPos, face);
                    break;
                case ABORT_BREAK:
                case STOP_BREAK:
                    handleStopBreak(blockPos);
                    break;
                case CONTINUE_BREAK:
                    handleContinueBreak(blockPos, face);
                    break;
                case BLOCK_PREDICT_DESTROY:
                    handleBlockPredictDestroy(blockPos, face);
                    break;
                default:
                    break;
            }
        }
    }

    private void handleStartBreak(Vector3i blockPos, Direction face) {
        long currentBreak = System.currentTimeMillis();
        if ((lastBreakPosition.equals(blockPos) && (currentBreak - player.lastBreak) < 10) || player.getPosition().distanceSquared(blockPos.toFloat()) > 100) {
            return;
        }
        Block target = player.getLevel().getBlock(blockPos);
        BlockState targetState = target.getState();

        PlayerInteractEvent playerInteractEvent = new PlayerInteractEvent(player, player.getInventory().getSelectedItem(), target, face, targetState == BlockStates.AIR ? PlayerInteractEvent.Action.LEFT_CLICK_AIR : PlayerInteractEvent.Action.LEFT_CLICK_BLOCK);
        player.getServer().getEventManager().fire(playerInteractEvent);
        if (playerInteractEvent.isCancelled()) {
            player.getInventoryManager().sendAllInventories();
            return;
        }

        Block block = target.getSide(face);
        if (block.getState().getType() == BlockTypes.FIRE) {
            block.set(BlockStates.AIR, true);
            player.getLevel().addLevelSoundEvent(block.getPosition(), SoundEvent.EXTINGUISH_FIRE);
            return;
        }
        if (!player.isCreative()) {
            double breakTime = Math.ceil(CloudBlockRegistry.REGISTRY.getComponent(targetState.getType(), BlockComponents.GET_DESTROY_SPEED).execute(targetState) * 20);
            if (breakTime > 0) {
                LevelEventPacket levelEvent = new LevelEventPacket();
                levelEvent.setType(LevelEvent.BLOCK_START_BREAK);
                levelEvent.setPosition(blockPos.toFloat());
                levelEvent.setData((int) (65535 / breakTime));
                player.sendPacket(levelEvent);
                player.getLevel().addChunkPacket(blockPos, levelEvent);
            }
        }

        player.breakingBlock = target;
        player.lastBreak = currentBreak;
        lastBreakPosition = blockPos;
    }

    private void handleStopBreak(Vector3i blockPos) {
        LevelEventPacket levelEvent = new LevelEventPacket();
        levelEvent.setType(LevelEvent.BLOCK_STOP_BREAK);
        levelEvent.setPosition(blockPos.toFloat());
        levelEvent.setData(0);
        player.sendPacket(levelEvent);
        player.getLevel().addChunkPacket(blockPos, levelEvent);
        player.breakingBlock = null;
    }

    private void handleContinueBreak(Vector3i blockPos, Direction face) {
        if (player.isBreakingBlock()) {
            Block block = player.getLevel().getBlock(blockPos);
            player.getLevel().addParticle(new PunchBlockParticle(blockPos.toFloat(), block.getState(), face));
        }
    }

    private void handleBlockPredictDestroy(Vector3i blockPos, Direction face) {
        if (!player.spawned || !player.isAlive()) {
            return;
        }

        player.breakingBlock = null;

        LevelEventPacket levelEvent = new LevelEventPacket();
        levelEvent.setType(LevelEvent.BLOCK_STOP_BREAK);
        levelEvent.setPosition(blockPos.toFloat());
        levelEvent.setData(0);
        player.getLevel().addChunkPacket(blockPos, levelEvent);

        ItemStack selectedItem = player.getInventory().getSelectedItem();
        ItemStack oldItem = selectedItem;

        if (player.canInteract(blockPos.toFloat().add(0.5f, 0.5f, 0.5f), player.isCreative() ? 13 : 7)) {
            selectedItem = player.getLevel().useBreakOn(blockPos, face, selectedItem, player, true);
            if (selectedItem != null) {
                if (player.isSurvival() || player.isAdventure()) {
                    player.getFoodData().updateFoodExpLevel(0.025);
                    if (!selectedItem.equals(oldItem) || selectedItem.getCount() != oldItem.getCount()) {
                        player.getInventory().setSelectedItem(selectedItem);
                    }
                }
                return;
            }
        }

        player.sendInventoryContents();
        Block target = player.getLevel().getBlock(blockPos);
        BlockEntity blockEntity = player.getLevel().getLoadedBlockEntity(blockPos);

        player.getLevel().sendBlocks(new CloudPlayer[]{player}, new Block[]{target}, UpdateBlockPacket.FLAG_ALL_PRIORITY);

        if (blockEntity != null && blockEntity.isSpawnable()) {
            blockEntity.spawnTo(player);
        }
    }

    private void processItemUseTransaction(PlayerAuthInputPacket packet) {
        ItemUseTransaction transaction = packet.getItemUseTransaction();
        if (transaction == null) {
            return;
        }

        Vector3i blockPos = transaction.getBlockPosition();
        int blockFace = transaction.getBlockFace();
        Direction face = Direction.fromIndex(blockFace);
        Vector3f clickPos = transaction.getClickPosition();

        switch (transaction.getActionType()) {
            case 0:
                handleItemUseOnBlock(blockPos, face, clickPos);
                break;
            case 1:
                handleItemUseInAir(face);
                break;
            case 2:
                break;
            default:
                break;
        }
    }

    private void handleItemUseOnBlock(Vector3i blockPos, Direction face, Vector3f clickPos) {
        boolean spamBug = (lastRightClickPos != null
                && System.currentTimeMillis() - lastRightClickTime < 100.0
                && blockPos.distanceSquared(lastRightClickPos) < 0.00001);
        lastRightClickPos = blockPos;
        lastRightClickTime = System.currentTimeMillis();
        if (spamBug) {
            return;
        }

        player.setUsingItem(false);

        if (!player.canInteract(blockPos.toFloat().add(0.5f, 0.5f, 0.5f), player.isCreative() ? 13 : 7)) {
            rollbackBlock(blockPos, face);
            return;
        }

        ItemStack serverItem = player.getInventory().getSelectedItem();
        ItemStack oldItem = serverItem;

        if (player.isCreative()) {
            ItemStack result = player.getLevel().useItemOn(blockPos, serverItem, face, clickPos, player);
            if (result != null) {
                return;
            }
        } else {
            ItemStack result = player.getLevel().useItemOn(blockPos, serverItem, face, clickPos, player);
            if (result != null) {
                if (!result.equals(oldItem) || result.getCount() != oldItem.getCount()) {
                    player.getInventory().setSelectedItem(result);
                }
                return;
            }
        }

        rollbackBlock(blockPos, face);
    }

    private void handleItemUseInAir(Direction face) {
        Vector3f directionVector = player.getDirectionVector();

        ItemStack serverItem = player.getInventory().getSelectedItem();

        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, serverItem, directionVector, face, PlayerInteractEvent.Action.RIGHT_CLICK_AIR);
        player.getServer().getEventManager().fire(interactEvent);

        if (interactEvent.isCancelled()) {
            player.sendHeldItemSlot();
        }
    }

    private void rollbackBlock(Vector3i blockPos, Direction face) {
        if (blockPos.distanceSquared(player.getPosition().toInt()) > 10000) {
            return;
        }
        Block target = player.getLevel().getBlock(blockPos);
        Block adjacent = target.getSide(face);
        player.getLevel().sendBlocks(new CloudPlayer[]{player}, new Block[]{target, adjacent}, UpdateBlockPacket.FLAG_ALL_PRIORITY);
    }

    private void processInputFlags(Set<PlayerAuthInputData> inputData) {
        for (PlayerAuthInputData input : inputData) {
            switch (input) {
                case START_SPRINTING:
                    PlayerToggleSprintEvent sprintEvent = new PlayerToggleSprintEvent(player, true);
                    player.getServer().getEventManager().fire(sprintEvent);
                    if (sprintEvent.isCancelled()) {
                        player.sendFlags(player);
                    } else {
                        player.setSprinting(true);
                    }
                    break;
                case STOP_SPRINTING:
                    sprintEvent = new PlayerToggleSprintEvent(player, false);
                    player.getServer().getEventManager().fire(sprintEvent);
                    if (sprintEvent.isCancelled()) {
                        player.sendFlags(player);
                    } else {
                        player.setSprinting(false);
                    }
                    if (player.isSwimming()) {
                        PlayerToggleSwimEvent ptse = new PlayerToggleSwimEvent(player, false);
                        player.getServer().getEventManager().fire(ptse);
                        if (ptse.isCancelled()) {
                            player.sendFlags(player);
                        } else {
                            player.setSwimming(false);
                        }
                    }
                    break;
                case START_SNEAKING:
                    PlayerToggleSneakEvent sneakEvent = new PlayerToggleSneakEvent(player, true);
                    player.getServer().getEventManager().fire(sneakEvent);
                    if (sneakEvent.isCancelled()) {
                        player.sendFlags(player);
                    } else {
                        player.setSneaking(true);
                    }
                    break;
                case STOP_SNEAKING:
                    sneakEvent = new PlayerToggleSneakEvent(player, false);
                    player.getServer().getEventManager().fire(sneakEvent);
                    if (sneakEvent.isCancelled()) {
                        player.sendFlags(player);
                    } else {
                        player.setSneaking(false);
                    }
                    break;
                case START_SWIMMING:
                    PlayerToggleSwimEvent swimEvent = new PlayerToggleSwimEvent(player, true);
                    player.getServer().getEventManager().fire(swimEvent);
                    if (swimEvent.isCancelled()) {
                        player.sendFlags(player);
                    } else {
                        player.setSwimming(true);
                    }
                    break;
                case STOP_SWIMMING:
                    swimEvent = new PlayerToggleSwimEvent(player, false);
                    player.getServer().getEventManager().fire(swimEvent);
                    if (swimEvent.isCancelled()) {
                        player.sendFlags(player);
                    } else {
                        player.setSwimming(false);
                    }
                    break;
                case START_GLIDING:
                    PlayerToggleGlideEvent glideEvent = new PlayerToggleGlideEvent(player, true);
                    player.getServer().getEventManager().fire(glideEvent);
                    if (glideEvent.isCancelled()) {
                        player.sendFlags(player);
                    } else {
                        player.setGliding(true);
                    }
                    break;
                case STOP_GLIDING:
                    glideEvent = new PlayerToggleGlideEvent(player, false);
                    player.getServer().getEventManager().fire(glideEvent);
                    if (glideEvent.isCancelled()) {
                        player.sendFlags(player);
                    } else {
                        player.setGliding(false);
                    }
                    break;
                case START_JUMPING:
                    player.getServer().getEventManager().fire(new PlayerJumpEvent(player));
                    break;
                case MISSED_SWING:
                    AnimatePacket animatePacket = new AnimatePacket();
                    animatePacket.setAction(AnimatePacket.Action.SWING_ARM);
                    animatePacket.setRuntimeEntityId(player.getRuntimeId());
                    CloudServer.broadcastPacket(player.getViewers(), animatePacket);
                    player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.ATTACK_NODAMAGE, -1, Identifier.parse("minecraft:player"), false, false);
                    break;
                case START_FLYING:
                    if (!player.getServer().getAllowFlight() && !player.getAdventureSettings().get(org.cloudburstmc.api.player.AdventureSetting.ALLOW_FLIGHT)) {
                        player.kick(PlayerKickEvent.Reason.FLYING_DISABLED, "Flying is not enabled on player server");
                    } else {
                        PlayerToggleFlightEvent flightEvent = new PlayerToggleFlightEvent(player, true);
                        player.getServer().getEventManager().fire(flightEvent);
                        if (flightEvent.isCancelled()) {
                            player.getAdventureSettings().update();
                        } else {
                            player.getAdventureSettings().set(org.cloudburstmc.api.player.AdventureSetting.FLYING, true);
                        }
                    }
                    break;
                case STOP_FLYING:
                    PlayerToggleFlightEvent flightEvent = new PlayerToggleFlightEvent(player, false);
                    player.getServer().getEventManager().fire(flightEvent);
                    if (flightEvent.isCancelled()) {
                        player.getAdventureSettings().update();
                    } else {
                        player.getAdventureSettings().set(org.cloudburstmc.api.player.AdventureSetting.FLYING, false);
                    }
                    break;
                default:
                    break;
            }
        }
        player.getData().update();
        player.setUsingItem(false);
    }

    @Override
    public PacketSignal handle(AdventureSettingsPacket packet) {
        Set<AdventureSetting> flags = packet.getSettings();
        if (!player.getServer().getAllowFlight() && flags.contains(AdventureSetting.FLYING) && !player.getAdventureSettings().get(org.cloudburstmc.api.player.AdventureSetting.ALLOW_FLIGHT)) {
            player.kick(PlayerKickEvent.Reason.FLYING_DISABLED, "Flying is not enabled on player server");
            return PacketSignal.HANDLED;
        }
        PlayerToggleFlightEvent playerToggleFlightEvent = new PlayerToggleFlightEvent(player, flags.contains(AdventureSetting.FLYING));
        player.getServer().getEventManager().fire(playerToggleFlightEvent);
        if (playerToggleFlightEvent.isCancelled()) {
            player.getAdventureSettings().update();
        } else {
            player.getAdventureSettings().set(org.cloudburstmc.api.player.AdventureSetting.FLYING, playerToggleFlightEvent.isFlying());
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(EmotePacket packet) {
        if (!player.isSpawned()) {
            return PacketSignal.HANDLED;
        }
        if (packet.getRuntimeEntityId() != player.getRuntimeId()) {
            log.warn(player.getName() + " sent EmotePacket with invalid entity id: " + packet.getRuntimeEntityId() + " != " + player.getRuntimeId());
            return PacketSignal.HANDLED;
        }
        for (CloudPlayer p : this.player.getViewers()) {
            p.sendPacket(packet);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(PacketViolationWarningPacket packet) {
        log.warn("Recived Packet Violation Warning: {}", packet.toString());
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(MobEquipmentPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return PacketSignal.HANDLED;
        }

        boolean offhand = packet.getContainerId() == ContainerId.OFFHAND;
        ItemStack serverItem;
        if (offhand) {
            serverItem = player.getOffhand().getOffhandItem();
        } else {
            serverItem = player.getContainer().getItem(packet.getHotbarSlot());
        }
        ItemStack clientItem = ItemUtils.fromNetwork(packet.getItem());

        if (!serverItem.isSimilar(clientItem)) {
            log.debug("Tried to equip " + clientItem + " but have " + serverItem + " in target slot");
            player.getInventoryManager().sendAllInventories();
            return PacketSignal.HANDLED;
        }
        if (offhand) {
            player.getOffhand().setOffhandItem(serverItem);
        } else {
            player.setSelectedHotbarSlot(packet.getHotbarSlot());
        }
        player.setUsingItem(false);

        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(PlayerActionPacket packet) {
        if (!player.spawned || (!player.isAlive() &&
                packet.getAction() != PlayerActionType.RESPAWN &&
                packet.getAction() != PlayerActionType.DIMENSION_CHANGE_REQUEST_OR_CREATIVE_DESTROY_BLOCK)) {
            return PacketSignal.HANDLED;
        }

        packet.setRuntimeEntityId(player.getRuntimeId());

        switch (packet.getAction()) {
            case GET_UPDATED_BLOCK:
                break; //TODO
            case DROP_ITEM:
                break; //TODO
            case STOP_SLEEP:
                player.stopSleep();
                break;
            case RESPAWN:
                if (!player.spawned || player.isAlive() || !player.isOnline()) {
                    break;
                }

                if (player.getServer().isHardcore()) {
                    player.setBanned(true);
                    break;
                }

                player.closeInventory();

                PlayerRespawnEvent playerRespawnEvent = new PlayerRespawnEvent(player, player.getSpawn());
                player.getServer().getEventManager().fire(playerRespawnEvent);

                Location respawnLoc = playerRespawnEvent.getRespawnLocation();

                player.teleport(respawnLoc, null);

                player.setSprinting(false);
                player.setSneaking(false);

                player.getData().set(EntityDataTypes.AIR_SUPPLY, (short) 400);
                player.deadTicks = 0;
                player.noDamageTicks = 60;

                player.removeAllEffects();
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setLevel(20, 20);

                player.sendData(player);

                player.setMovementSpeed(DEFAULT_SPEED);

                player.getAdventureSettings().update();
                player.getInventoryManager().sendAllInventories();

                player.spawnToAll();
                player.scheduleUpdate();
                break;
            case DIMENSION_CHANGE_REQUEST_OR_CREATIVE_DESTROY_BLOCK:
            default:
                break;
        }

        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ModalFormResponsePacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return PacketSignal.HANDLED;
        }

        Form<?> window = player.removeFormWindow(packet.getFormId());

        if (window == null) {
            if (player.getServerSettings() != null && player.getServerSettingsId() == packet.getFormId()) {
                window = player.getServerSettings();
            } else {
                return PacketSignal.HANDLED;
            }
        }

        try {
            JsonNode response = new JsonMapper().readTree(packet.getFormData());

            if ("null".equals(response.asText())) {
                window.close(player);
            } else {
                try {
                    window.handleResponse(player, response);
                } catch (Exception e) {
                    log.error("Error while handling form response", e);
                    window.error(player);
                }
            }
        } catch (JacksonException e) {
            log.debug("Received corrupted form json data");
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(InteractPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return PacketSignal.HANDLED;
        }

        // player.getCraftingInventory().resetCraftingGrid();

        Entity targetEntity = player.getLevel().getEntity(packet.getRuntimeEntityId());

        if (targetEntity == null || !player.isAlive() || !targetEntity.isAlive()) {
            return PacketSignal.HANDLED;
        }

        if (targetEntity instanceof DroppedItem || targetEntity instanceof EntityArrow || targetEntity instanceof ExperienceOrb) {
            player.kick(PlayerKickEvent.Reason.INVALID_PVE, "Attempting to interact with an invalid entity");
            log.warn(player.getServer().getLanguage().translate("cloudburst.player.invalidEntity", player.getName()));
            return PacketSignal.HANDLED;
        }

        switch (packet.getAction()) {
            case MOUSEOVER:
                if (packet.getRuntimeEntityId() == 0) {
                    break;
                }
                player.getServer().getEventManager().fire(new PlayerMouseOverEntityEvent(player, targetEntity));
                break;
            case LEAVE_VEHICLE:
                if (player.getVehicle() == null) {
                    break;
                }
                player.dismount(player.getVehicle());
                break;
            case OPEN_INVENTORY:
                if (targetEntity.getRuntimeId() != player.getRuntimeId()) break;
                if (player.canOpenInventory()) {
                    player.getInventoryManager().openScreen(new CloudPlayerInventoryScreen(player));

                    ContainerOpenPacket containerOpen = new ContainerOpenPacket();
                    containerOpen.setId((byte) ContainerId.INVENTORY);
                    containerOpen.setType(ContainerType.INVENTORY);
                    containerOpen.setUniqueEntityId(-1);
                    containerOpen.setBlockPosition(player.getPosition().toInt());
                    player.sendPacket(containerOpen);
                    player.getInventoryManager().sendAllInventories();
                }
                break;
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(BlockPickRequestPacket packet) {
        if (player.isSpectator()) {
            log.debug("Got block-pick request from " + player.getName() + " when in spectator mode");
            return PacketSignal.HANDLED;
        }

        Vector3i pickPos = packet.getBlockPosition();
        Block block = player.getLevel().getBlock(pickPos.getX(), pickPos.getY(), pickPos.getZ());

        if (block.getState().getType() == BlockTypes.AIR) {
            log.debug("Got block-pick request from " + player.getName() + " for air block");
            return PacketSignal.HANDLED;
        }

        ItemStack item = ItemStack.from(block.getState());

        if (packet.isAddUserData()) {
            BaseBlockEntity blockEntity = (BaseBlockEntity) player.getLevel().getLoadedBlockEntity(
                    Vector3i.from(pickPos.getX(), pickPos.getY(), pickPos.getZ()));
            if (blockEntity != null) {
                NbtMap nbt = blockEntity.getItemTag();
                if (nbt != null) {
//                    ItemStackBuilder builder = (ItemStackBuilder) item.toBuilder(); //TODO
//                    builder.nbt()
//                    item.addTag(nbt);
//                    item.setLore("+(DATA)");
                }
            }
        }

        PlayerBlockPickEvent pickEvent = new PlayerBlockPickEvent(player, block, item);
        player.getServer().getEventManager().fire(pickEvent);

        if (!pickEvent.isCancelled()) {
            item = pickEvent.getItem();
            boolean itemExists = false;
            int itemSlot = -1;
            for (int slot = 0; slot < player.getContainer().size(); slot++) {
                if (player.getContainer().getItem(slot).isSimilar(item)) {
                    if (slot < player.getInventory().getHotbarSize()) {
                        player.setSelectedHotbarSlot(slot);
                    } else {
                        itemSlot = slot;
                    }
                    itemExists = true;
                    break;
                }
            }

            for (int slot = 0; slot < player.getInventory().getHotbarSize(); slot++) {
                if (player.getContainer().getItem(slot).isEmpty()) {
                    if (!itemExists && player.isCreative()) {
                        player.getInventory().setSelectedSlot(slot);
                        player.getInventory().setSelectedItem(item);
                        return PacketSignal.HANDLED;
                    } else if (itemSlot > -1) {
                        player.getInventory().setSelectedSlot(slot);
                        player.getInventory().setSelectedItem(player.getContainer().getItem(itemSlot));
                        player.getInventory().setItem(itemSlot, ItemStack.EMPTY);
                        return PacketSignal.HANDLED;
                    }
                }
            }

            if (!itemExists && player.isCreative()) {
                ItemStack itemInHand = player.getInventory().getSelectedItem();
                player.getInventory().setSelectedItem(item);
                if (!player.getContainer().isFull()) {
                    for (int slot = 0; slot < player.getContainer().size(); slot++) {
                        if (player.getContainer().getItem(slot).isEmpty()) {
                            player.getContainer().setItem(slot, itemInHand);
                            break;
                        }
                    }
                }
            } else if (itemSlot > -1) {
                ItemStack itemInHand = player.getInventory().getSelectedItem();
                player.getInventory().setSelectedItem(player.getContainer().getItem(itemSlot));
                player.getContainer().setItem(itemSlot, itemInHand);
            }
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(AnimatePacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return PacketSignal.HANDLED;
        }

        PlayerAnimationEvent animationEvent = new PlayerAnimationEvent(player, PlayerAnimationEvent.Type.values()[packet.getAction().ordinal()]);
        player.getServer().getEventManager().fire(animationEvent);
        if (animationEvent.isCancelled()) {
            return PacketSignal.HANDLED;
        }

        AnimatePacket.Action animation = AnimatePacket.Action.values()[animationEvent.getAnimationType().ordinal()];

        switch (animation) {
            case ROW_RIGHT:
            case ROW_LEFT:
                if (player.getVehicle() instanceof EntityBoat) {
                    ((EntityBoat) player.getVehicle()).onPaddle(animation, packet.getRowingTime());
                }
                break;
        }

        AnimatePacket animatePacket = new AnimatePacket();
        animatePacket.setRuntimeEntityId(player.getRuntimeId());
        animatePacket.setAction(AnimatePacket.Action.values()[animationEvent.getAnimationType().ordinal()]);
        CloudServer.broadcastPacket(player.getViewers(), animatePacket);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(SetHealthPacket packet) {
        // Cannot be trusted. Use UpdateAttributePacket instead
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(EntityEventPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return PacketSignal.HANDLED;
        }

        //player.getCraftingInventory().resetCraftingGrid();

        if (packet.getType() == EntityEventType.EATING_ITEM) {
            if (packet.getData() == 0 || packet.getRuntimeEntityId() != player.getRuntimeId()) {
                return PacketSignal.HANDLED;
            }

            packet.setRuntimeEntityId(player.getRuntimeId());

            player.sendPacket(packet);
            CloudServer.broadcastPacket(player.getViewers(), packet);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(CommandRequestPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return PacketSignal.HANDLED;
        }
        PlayerCommandPreprocessEvent playerCommandPreprocessEvent = new PlayerCommandPreprocessEvent(player, packet.getCommand());
        player.getServer().getEventManager().fire(playerCommandPreprocessEvent);
        if (playerCommandPreprocessEvent.isCancelled()) {
            return PacketSignal.HANDLED;
        }

        try (Timing ignored2 = Timings.playerCommandTimer.startTiming()) {
            player.getServer().dispatchCommand((CommandSender) playerCommandPreprocessEvent.getPlayer(), playerCommandPreprocessEvent.getMessage().substring(1));
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(TextPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return PacketSignal.HANDLED;
        }

        if (packet.getType() == TextPacket.Type.CHAT) {
            player.chat(packet.getMessage());
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ContainerClosePacket packet) {
        if (!player.spawned) {
            return PacketSignal.HANDLED;
        }

        player.handleClientContainerClose(packet);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(CraftingEventPacket packet) {
//        CraftingRecipe recipe = (CraftingRecipe) CloudRecipeRegistry.get().getRecipe(packet.getUuid());
//        if (recipe != null) {
//            CraftItemStackTransaction transaction = new CraftItemStackTransaction(player, recipe);
//            transaction.setPrimaryOutput(ItemUtils.fromNetwork(packet.getOutputs().remove(0)));
//            if (packet.getOutputs().size() >= 1) {
//                int slot = 0;
//                for (ItemData data : packet.getOutputs()) {
//                    transaction.setExtraOutput(slot++, ItemUtils.fromNetwork(data));
//                }
//            }
//            player.getInventoryManager().setTransaction(transaction);
//            return PacketSignal.HANDLED;
//        }
//        log.warn("Received invalid recipe UUID({}) in CraftingEventPacket", packet.getUuid());
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(BlockEntityDataPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return PacketSignal.HANDLED;
        }

        // player.getCraftingInventory().resetCraftingGrid();

        Vector3i blockPos = packet.getBlockPosition();
        if (blockPos.distanceSquared(player.getPosition().toInt()) > 10000) {
            return PacketSignal.HANDLED;
        }

        BaseBlockEntity blockEntity = (BaseBlockEntity) player.getLevel().getLoadedBlockEntity(blockPos);
        if (blockEntity != null && blockEntity.isSpawnable()) {
            if (!blockEntity.updateFromClient(packet.getData(), player)) {
                blockEntity.spawnTo(player);
            }
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(RequestChunkRadiusPacket packet) {
        player.getChunkManager().setChunkRadius(packet.getRadius());
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(SetPlayerGameTypePacket packet) {
        if (packet.getGamemode() != player.getGamemode().getVanillaId()) {
            if (!player.hasPermission("cloudburst.command.gamemode")) {
                SetPlayerGameTypePacket packet1 = new SetPlayerGameTypePacket();
                packet1.setGamemode(player.getGamemode().getVanillaId());
                player.sendPacket(packet1);
                player.getAdventureSettings().update();
                return PacketSignal.HANDLED;
            }
            player.setGamemode(GameMode.from(packet.getGamemode()), true);
            CommandUtils.broadcastCommandMessage(player, new TranslationContainer("%commands.gamemode.success.self", player.getGamemode().getTranslation()));
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ItemFrameDropItemPacket packet) {
        Vector3i vector3 = packet.getBlockPosition();
        BlockEntity blockEntity = player.getLevel().getLoadedBlockEntity(vector3);
        if (!(blockEntity instanceof ItemFrame itemFrame)) {
            return PacketSignal.HANDLED;
        }
        Block block = itemFrame.getBlock();
        ItemStack itemDrop = itemFrame.getItem();
        ItemFrameDropItemEvent itemFrameDropItemEvent = new ItemFrameDropItemEvent(player, block, itemFrame, itemDrop);
        player.getServer().getEventManager().fire(itemFrameDropItemEvent);
        if (!itemFrameDropItemEvent.isCancelled()) {
            if (!itemDrop.isEmpty()) {
                player.getLevel().dropItem(itemFrame.getPosition(), itemDrop);
                itemFrame.setItem(ItemStack.EMPTY);
                itemFrame.setItemRotation(0);
                player.getLevel().addSound(player.getPosition(), Sound.BLOCK_ITEMFRAME_REMOVE_ITEM);
            }
        } else {
            itemFrame.spawnTo(player);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(MapInfoRequestPacket packet) {
        ItemStack mapItem = null;

        for (ItemStack item1 : player.getContainer().getContents()) {
            if (item1.getType() != ItemTypes.FILLED_MAP) {
                continue;
            }

            MapItem data = item1.get(ItemKeys.MAP_DATA);

            if (data == null) {
                continue;
            }

            if (data.getId() == packet.getUniqueMapId()) {
                mapItem = item1;
                break;
            }
        }

        if (mapItem == null) {
            for (BlockEntity be : player.getLevel().getBlockEntities()) {
                if (be instanceof ItemFrame itemFrame1) {
                    ItemStack frameItem = itemFrame1.getItem();
                    if (frameItem.getType() != ItemTypes.FILLED_MAP) {
                        continue;
                    }

                    MapItem data = itemFrame1.getItem().get(ItemKeys.MAP_DATA);

                    if (data == null) {
                        continue;
                    }

                    if (data.getId() == packet.getUniqueMapId()) {
//                        ((ItemMapBehavior) itemFrame1.getItem()).sendImage(player); //TODO: send image
                        break;
                    }
                }
            }

            return PacketSignal.HANDLED;
        }

        PlayerMapInfoRequestEvent event;
        player.getServer().getEventManager().fire(event = new PlayerMapInfoRequestEvent(player, mapItem));

        if (!event.isCancelled()) {
//                ((ItemMapBehavior) mapItem).sendImage(player); //TODO: send image
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ItemStackRequestPacket packet) {
        this.player.getItemStackNetManager().handlePacket(packet);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(InventoryTransactionPacket packet) {
        if (player.isSpectator() || !player.spawned || !player.isAlive()) {
            player.getInventoryManager().sendAllInventories();
            return PacketSignal.HANDLED;
        }

        if (packet.getTransactionType() == InventoryTransactionType.NORMAL && packet.getActions().size() == 3) {
            InventoryActionData bookAction = packet.getActions().get(0);
            if (bookAction.getSource().getType() == InventorySource.Type.CONTAINER
                    && player.getSelectedHotbarSlot() == bookAction.getSlot()
                    && ItemUtils.fromNetwork(bookAction.getFromItem()).getType() == ItemTypes.WRITABLE_BOOK) {
                return PacketSignal.HANDLED;
            }
        }

        switch (packet.getTransactionType()) {
            case NORMAL:
                if (packet.getActions().size() == 2) {
                    InventoryActionData worldAction = null;
                    InventoryActionData containerAction = null;

                    for (InventoryActionData action : packet.getActions()) {
                        if (action.getSource().getType() == InventorySource.Type.WORLD_INTERACTION && action.getSource().getFlag() == InventorySource.Flag.DROP_ITEM) {
                            worldAction = action;
                        } else {
                            containerAction = action;
                        }
                    }

                    if (worldAction != null && containerAction != null) {
                        int slot = containerAction.getSlot();
                        ItemStack currentItem = player.getContainer().getItem(slot);

                        if (currentItem.isEmpty()) {
                            return PacketSignal.HANDLED;
                        }

                        boolean dropAll = worldAction.getToItem().getCount() > 1;
                        ItemStack dropItem = dropAll ? currentItem : currentItem.withCount(1);
                        ItemStack newItem = dropAll ? ItemStack.EMPTY : currentItem.withCount(currentItem.getCount() - 1);

                        player.getContainer().setItem(slot, newItem);
                        player.dropItem(dropItem);
                        return PacketSignal.HANDLED;
                    }
                }
                log.debug("Received NORMAL inventory transaction from {} - currently not handled", player.getName());
                return PacketSignal.HANDLED;
            case INVENTORY_MISMATCH:
                player.getInventoryManager().sendAllInventories();
                return PacketSignal.HANDLED;
            case ITEM_USE:
                switch (packet.getActionType()) {
                    case 0: // use on block
                        handleItemUseOnBlock(
                                packet.getBlockPosition(),
                                Direction.fromIndex(packet.getBlockFace()),
                                packet.getClickPosition()
                        );
                        break;
                    case 1: // use in air
                        handleItemUseInAir(Direction.fromIndex(packet.getBlockFace()));
                        break;
                    case 2: // break-block no-op
                        break;
                    case 3:
                        // TODO: implement piercing-weapon stab (trident melee in water)
                        break;
                    default:
                        break;
                }
                return PacketSignal.HANDLED;
            case ITEM_USE_ON_ENTITY:
                // TODO: implement entity interaction (interact + attack) and entity stabbing (piercing weapon / trident)
                log.debug("Received ITEM_USE_ON_ENTITY from {} - currently not handled", player.getName());
                break;
            case ITEM_RELEASE:
                // TODO: implement item release (stop using / release bow, crossbow, trident, food)
                log.debug("Received ITEM_RELEASE from {} - currently not handled", player.getName());
                break;
            default:
                player.getInventoryManager().sendAllInventories();
                break;
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(PlayerHotbarPacket packet) {
        if (packet.getContainerId() != ContainerId.INVENTORY) {
            return PacketSignal.HANDLED; // This should never happen
        }

        player.setSelectedHotbarSlot(packet.getSelectedHotbarSlot());
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ServerSettingsRequestPacket packet) {
        CustomForm settings = player.getServerSettings();

        if (settings == null) {
            return PacketSignal.HANDLED;
        }

        try {
            ServerSettingsResponsePacket re = new ServerSettingsResponsePacket();
            re.setFormId(player.getServerSettingsId());
            re.setFormData(new JsonMapper().writeValueAsString(settings));
            player.sendPacket(re);
        } catch (JacksonException e) {
            log.error("Error while writing form data", e);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(RespawnPacket packet) {
        if (player.isAlive()) {
            return PacketSignal.HANDLED;
        }
        if (packet.getState() == RespawnPacket.State.CLIENT_READY) {
            RespawnPacket respawn1 = new RespawnPacket();
            respawn1.setPosition(player.getSpawn().getPosition());
            respawn1.setState(RespawnPacket.State.SERVER_READY);
            player.sendPacket(respawn1);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LecternUpdatePacket packet) {
        Vector3i blockPosition = packet.getBlockPosition();

        if (packet.isDroppingBook()) {
            Block block = player.getLevel().getBlock(blockPosition);
            BlockState state = block.getState();
            if (state.getType() == BlockTypes.LECTERN) {
//                TODO Drop Lectern book
//                ((BlockBehaviorLectern) state.getBehavior()).dropBook(block, player);
            }
        } else {
            BlockEntity blockEntity = player.getLevel().getBlockEntity(blockPosition);
            if (blockEntity instanceof Lectern lectern) {
                LecternPageChangeEvent lecternPageChangeEvent = new LecternPageChangeEvent(player, lectern, packet.getPage());
                player.getServer().getEventManager().fire(lecternPageChangeEvent);
                if (!lecternPageChangeEvent.isCancelled()) {
                    lectern.setPage(lecternPageChangeEvent.getNewRawPage());
                    lectern.spawnToAll();
                    Block block = lectern.getBlock();
                    BlockState state = block.getState();
                    if (state.getType() == BlockTypes.LECTERN) {
                        block.getComponents().get(BlockComponents.ON_REDSTONE_UPDATE).execute(block);
//                        ((BlockBehaviorLectern) state.getBehavior()).executeRedstonePulse(block);
                    }
                }
            }
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(SetLocalPlayerAsInitializedPacket packet) {
        if (player.isInitialized()) {
            return PacketSignal.HANDLED;
        }
        player.setInitialized(true);
        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(player,
                new TranslationContainer(TextFormat.YELLOW + "%multiplayer.player.joined", player.getDisplayName())
        );

        player.getServer().getEventManager().fire(playerJoinEvent);

        if (playerJoinEvent.getJoinMessage().toString().trim().length() > 0) {
            player.getServer().broadcastMessage(playerJoinEvent.getJoinMessage());
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(SubChunkRequestPacket packet) {
        Vector3i center = packet.getSubChunkPosition();
        List<SubChunkData> responseChunks = new ArrayList<>(packet.getPositionOffsets().size());

        int minSectionY = this.player.getLevel().getMinSectionY();
        int maxSectionY = minSectionY + this.player.getLevel().getSectionsCount() - 1;

        for (Vector3i offset : packet.getPositionOffsets()) {
            int sectionY = center.getY() + offset.getY();
            int chunkX = center.getX() + offset.getX();
            int chunkZ = center.getZ() + offset.getZ();

            SubChunkData subChunkData = new SubChunkData();
            subChunkData.setPosition(offset);

            // Reject sections outside overworld bounds [-4, 19]
            if (sectionY < minSectionY || sectionY > maxSectionY) {
                subChunkData.setResult(SubChunkRequestResult.INDEX_OUT_OF_BOUNDS);
                subChunkData.setHeightMapType(HeightMapDataType.NO_DATA);
                subChunkData.setRenderHeightMapType(HeightMapDataType.NO_DATA);
                responseChunks.add(subChunkData);
                continue;
            }

            CloudLevel level = player.getLevel();
            CloudChunk chunk = level.getLoadedChunk(chunkX, chunkZ);

            if (chunk == null) {
                subChunkData.setResult(SubChunkRequestResult.CHUNK_NOT_FOUND);
                subChunkData.setHeightMapType(HeightMapDataType.NO_DATA);
                subChunkData.setRenderHeightMapType(HeightMapDataType.NO_DATA);
                responseChunks.add(subChunkData);
                continue;
            }

            // Array index for this sectionY
            int sectionIdx = sectionY - minSectionY;

            // Access sections under the read lock
            LockableChunk locked = chunk.readLockable();
            locked.lock();
            try {
                CloudChunkSection section = (CloudChunkSection) locked.getSection(sectionIdx);

                byte[] heightMap = new byte[256];
                boolean allHigher = true;
                boolean allLower = true;
                for (int hx = 0; hx < 16; hx++) {
                    for (int hz = 0; hz < 16; hz++) {
                        // World Y in [-64, 319]; -1 if the column is empty
                        int highestY = locked.getHighestBlock(hx, hz);
                        int heightSectionCoord;
                        if (highestY < 0) {
                            // Empty column — treat as below all sections
                            heightSectionCoord = minSectionY - 1;
                        } else {
                            heightSectionCoord = highestY >> 4;
                        }
                        int idx = (hz << 4) | hx;
                        if (heightSectionCoord > sectionY) {
                            heightMap[idx] = 16;
                            allLower = false;
                        } else if (heightSectionCoord < sectionY) {
                            heightMap[idx] = 0;
                            allHigher = false;
                        } else {
                            heightMap[idx] = (byte) (highestY & 0xf);
                            allHigher = false;
                            allLower = false;
                        }
                    }
                }

                HeightMapDataType hMapType;
                ByteBuf heightMapBuf;
                if (allHigher) {
                    hMapType = HeightMapDataType.TOO_HIGH;
                    heightMapBuf = Unpooled.EMPTY_BUFFER;
                } else if (allLower) {
                    hMapType = HeightMapDataType.TOO_LOW;
                    heightMapBuf = Unpooled.EMPTY_BUFFER;
                } else {
                    hMapType = HeightMapDataType.HAS_DATA;
                    heightMapBuf = Unpooled.copiedBuffer(heightMap);
                }
                subChunkData.setHeightMapType(hMapType);
                subChunkData.setHeightMapData(heightMapBuf);
                subChunkData.setRenderHeightMapType(hMapType);
                subChunkData.setRenderHeightMapData(heightMapBuf);

                if (section == null || section.isEmpty()) {
                    subChunkData.setResult(SubChunkRequestResult.SUCCESS_ALL_AIR);
                    subChunkData.setData(Unpooled.EMPTY_BUFFER);
                } else {
                    subChunkData.setResult(SubChunkRequestResult.SUCCESS);

                    ByteBuf sectionBuf = ByteBufAllocator.DEFAULT.ioBuffer();
                    section.writeToNetwork(sectionBuf, sectionY);

                    // Append block entities in this section
                    int minBlockY = sectionY * 16;
                    int maxBlockY = minBlockY + 15;
                    Set<? extends BlockEntity> tiles = locked.getBlockEntities();
                    if (!tiles.isEmpty()) {
                        try (ByteBufOutputStream stream = new ByteBufOutputStream(sectionBuf);
                             NBTOutputStream nbtOut = NbtUtils.createNetworkWriter(stream)) {
                            for (BlockEntity tile : tiles) {
                                Vector3i pos = tile.getPosition();
                                if (pos.getY() >= minBlockY && pos.getY() <= maxBlockY && tile instanceof BaseBlockEntity) {
                                    nbtOut.writeTag(((BaseBlockEntity) tile).getChunkTag());
                                }
                            }
                        } catch (IOException e) {
                            log.error("Error encoding block entity in sub-chunk ({},{},{})", chunkX, sectionY, chunkZ, e);
                        }
                    }

                    subChunkData.setData(sectionBuf);
                }
            } finally {
                locked.unlock();
            }

            responseChunks.add(subChunkData);
        }

        SubChunkPacket response = new SubChunkPacket();
        response.setDimension(packet.getDimension());
        response.setCenterPosition(center);
        response.setSubChunks(responseChunks);
        player.sendPacket(response);
        return PacketSignal.HANDLED;
    }
}
