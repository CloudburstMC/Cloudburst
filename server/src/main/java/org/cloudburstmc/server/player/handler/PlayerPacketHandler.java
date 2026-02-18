package org.cloudburstmc.server.player.handler;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockBehaviors;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
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
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.registry.GlobalRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.AdventureSetting;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.container.screen.CloudContainerScreen;
import org.cloudburstmc.server.container.screen.CloudInventoryScreen;
import org.cloudburstmc.server.entity.projectile.EntityArrow;
import org.cloudburstmc.server.entity.vehicle.EntityAbstractMinecart;
import org.cloudburstmc.server.entity.vehicle.EntityBoat;
import org.cloudburstmc.server.event.server.DataPacketReceiveEvent;
import org.cloudburstmc.server.form.CustomForm;
import org.cloudburstmc.server.form.Form;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.particle.PunchBlockParticle;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.utils.TextFormat;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.cloudburstmc.api.block.BlockTypes.AIR;
import static org.cloudburstmc.server.player.CloudPlayer.DEFAULT_SPEED;

@Log4j2
public class PlayerPacketHandler implements BedrockPacketHandler {
    private final CloudPlayer player;

    protected Vector3i lastRightClickPos = null;
    protected double lastRightClickTime = 0.0;

    private Vector3i lastBreakPosition = Vector3i.ZERO;

    @Inject
    GlobalRegistry globalRegistry;

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
        var targetState = target.getState();

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
            double breakTime = Math.ceil(CloudBlockRegistry.REGISTRY.getBehavior(targetState.getType(), BlockBehaviors.GET_DESTROY_SPEED).execute(targetState) * 20);
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
            ((BaseBlockEntity) blockEntity).spawnTo(player);
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

        if (!serverItem.equals(clientItem)) {
            log.debug("Tried to equip " + clientItem + " but have " + serverItem + " in target slot");
            player.getInventoryManager().sendAllInventories();
            return PacketSignal.HANDLED;
        }
        if (offhand) {
            player.getOffhand().setOffhandItem(clientItem);
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

                player.getInventoryManager().closeScreen();

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
                player.sendPosition(player.getPosition(), player.getYaw(), player.getPitch(), MovePlayerPacket.Mode.NORMAL);
                break; //TODO
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
                    player.getInventoryManager().openScreen(new CloudInventoryScreen(player));
                    player.openInventory(player);

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

        if (block.getState().getType() == AIR) {
            log.debug("Got block-pick request from " + player.getName() + " for air block");
            return PacketSignal.HANDLED;
        }

        ItemStack serverItem = ItemStack.from(block.getState());

        if (packet.isAddUserData()) {
            BaseBlockEntity blockEntity = (BaseBlockEntity) player.getLevel().getLoadedBlockEntity(
                    Vector3i.from(pickPos.getX(), pickPos.getY(), pickPos.getZ()));
            if (blockEntity != null) {
                NbtMap nbt = blockEntity.getItemTag();
                if (nbt != null) {
//                    ItemStackBuilder builder = (ItemStackBuilder) serverItem.toBuilder(); //TODO
//                    builder.nbt()
//                    serverItem.addTag(nbt);
//                    serverItem.setLore("+(DATA)");
                }
            }
        }

        PlayerBlockPickEvent pickEvent = new PlayerBlockPickEvent(player, block, serverItem);
        player.getServer().getEventManager().fire(pickEvent);

        if (!pickEvent.isCancelled()) {
            boolean itemExists = false;
            int itemSlot = -1;
            for (int slot = 0; slot < player.getContainer().size(); slot++) {
                if (player.getContainer().getItem(slot).equals(pickEvent.getItem())) {
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
                if (player.getContainer().getItem(slot) == ItemStack.EMPTY) {
                    if (!itemExists && player.isCreative()) {
                        player.getInventory().setSelectedSlot(slot);
                        player.getInventory().setSelectedItem(pickEvent.getItem());
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
                player.getInventory().setSelectedItem(pickEvent.getItem());
                if (!player.getContainer().isFull()) {
                    for (int slot = 0; slot < player.getContainer().size(); slot++) {
                        if (player.getContainer().getItem(slot) == ItemStack.EMPTY) {
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

        player.closeInventory(player);
        CloudContainerScreen screen = this.player.getInventoryManager().closeScreen();
        if (screen != null) {
            // TODO: Expose InventoryScreen to API
//            player.getServer().getEventManager().fire(new InventoryCloseEvent(screen, player));
        }

        player.sendPacket(packet);
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
        if (!(blockEntity instanceof ItemFrame)) {
            return PacketSignal.HANDLED;
        }
        ItemFrame itemFrame = (ItemFrame) blockEntity;
        Block block = itemFrame.getBlock();
        ItemStack itemDrop = itemFrame.getItem();
        ItemFrameDropItemEvent itemFrameDropItemEvent = new ItemFrameDropItemEvent(player, block, itemFrame, itemDrop);
        player.getServer().getEventManager().fire(itemFrameDropItemEvent);
        if (!itemFrameDropItemEvent.isCancelled()) {
            if (itemDrop.getType() != AIR) {
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
                if (be instanceof ItemFrame) {
                    ItemFrame itemFrame1 = (ItemFrame) be;
                    var frameItem = itemFrame1.getItem();

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
    public PacketSignal handle(LevelSoundEvent2Packet packet) {
        if (!player.isSpectator() || (packet.getSound() != SoundEvent.HIT &&
                packet.getSound() != SoundEvent.ATTACK_NODAMAGE)) {
            player.getLevel().addChunkPacket(player.getPosition(), packet);
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

        switch (packet.getTransactionType()) {
            case NORMAL:
                log.debug("Received NORMAL inventory transaction from {} - currently not handled", player.getName());
                return PacketSignal.HANDLED;
            case INVENTORY_MISMATCH:
                if (!packet.getActions().isEmpty()) {
                    log.debug("Expected 0 actions for mismatch, got {} {}", packet.getActions().size(), packet.getActions());
                }
                player.getInventoryManager().sendAllInventories();
                return PacketSignal.HANDLED;
            case ITEM_USE:
                Vector3i blockPos = packet.getBlockPosition();
                Direction face = Direction.fromIndex(packet.getBlockFace());

                switch (packet.getActionType()) {
                    case 0:
                        handleItemUseOnBlock(blockPos, face, packet.getClickPosition());
                        return PacketSignal.HANDLED;
                    case 1:
                        handleItemUseInAir(face);
                        return PacketSignal.HANDLED;
                    case 2:
                        log.debug("Received USE_ITEM_ACTION_BREAK_BLOCK via InventoryTransaction - ignoring (handled via PlayerAuthInput)");
                        return PacketSignal.HANDLED;
                    default:
                        log.debug("Unknown ITEM_USE actionType: {}", packet.getActionType());
                        break;
                }
                break;
            case ITEM_USE_ON_ENTITY:
                log.debug("Received ITEM_USE_ON_ENTITY from {} - currently not handled", player.getName());
                break;
            case ITEM_RELEASE:
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
            var state = block.getState();
            if (state.getType() == BlockTypes.LECTERN) {
//                TODO Drop Lectern book
//                ((BlockBehaviorLectern) state.getBehavior()).dropBook(block, player);
            }
        } else {
            BlockEntity blockEntity = player.getLevel().getBlockEntity(blockPosition);
            if (blockEntity instanceof Lectern) {
                Lectern lectern = (Lectern) blockEntity;
                LecternPageChangeEvent lecternPageChangeEvent = new LecternPageChangeEvent(player, lectern, packet.getPage());
                player.getServer().getEventManager().fire(lecternPageChangeEvent);
                if (!lecternPageChangeEvent.isCancelled()) {
                    lectern.setPage(lecternPageChangeEvent.getNewRawPage());
                    lectern.spawnToAll();
                    var block = lectern.getBlock();
                    var state = block.getState();
                    if (state.getType() == BlockTypes.LECTERN) {
                        block.getBehaviors().get(BlockBehaviors.ON_REDSTONE_UPDATE).execute(block);
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
}
