package org.cloudburstmc.server.player.handler;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.AdventureSetting;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.PlayerActionType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.ItemFrame;
import org.cloudburstmc.api.blockentity.Lectern;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.misc.ExperienceOrb;
import org.cloudburstmc.api.event.block.ItemFrameDropItemEvent;
import org.cloudburstmc.api.event.block.LecternPageChangeEvent;
import org.cloudburstmc.api.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.inventory.InventoryCloseEvent;
import org.cloudburstmc.api.event.player.*;
import org.cloudburstmc.api.inventory.CraftingGrid;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.Damageable;
import org.cloudburstmc.api.item.data.MapItem;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.block.behavior.BlockBehaviorLectern;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.entity.projectile.EntityArrow;
import org.cloudburstmc.server.entity.vehicle.EntityAbstractMinecart;
import org.cloudburstmc.server.entity.vehicle.EntityBoat;
import org.cloudburstmc.server.event.server.DataPacketReceiveEvent;
import org.cloudburstmc.server.form.CustomForm;
import org.cloudburstmc.server.form.Form;
import org.cloudburstmc.server.inventory.transaction.CraftingTransaction;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.inventory.transaction.action.InventoryAction;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.particle.PunchBlockParticle;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.network.protocol.types.InventoryTransactionUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.TextFormat;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.cloudburstmc.api.block.BlockTypes.AIR;
import static org.cloudburstmc.server.player.CloudPlayer.DEFAULT_SPEED;

/**
 * @author Extollite
 */
@Log4j2
public class PlayerPacketHandler implements BedrockPacketHandler {
    private final CloudPlayer player;

    protected Vector3i lastRightClickPos = null;
    protected double lastRightClickTime = 0.0;

    private Vector3i lastBreakPosition = Vector3i.ZERO;

    public PlayerPacketHandler(CloudPlayer player) {
        this.player = player;
    }

    public boolean handle(BedrockPacket packet) {
        if (!player.isConnected()) {
            return true;
        }

        try (Timing ignored = Timings.getReceiveDataPacketTiming(packet).startTiming()) {
            if (log.isTraceEnabled() && !player.getServer().isIgnoredPacket(packet.getClass())) {
                log.trace("Inbound {}: {}", player.getName(), packet);
            }

            DataPacketReceiveEvent receiveEvent = new DataPacketReceiveEvent(player, packet);
            player.getServer().getEventManager().fire(receiveEvent);
            if (receiveEvent.isCancelled()) {
                return true;
            }

            return packet.handle(this);
        }
    }

    @Override
    public boolean handle(PlayerSkinPacket packet) {
        SerializedSkin skin = packet.getSkin();


        if (!skin.isValid()) {
            return true;
        }

        PlayerChangeSkinEvent playerChangeSkinEvent = new PlayerChangeSkinEvent(player, player.getLoginChainData().getSkin());
        playerChangeSkinEvent.setCancelled(TimeUnit.SECONDS.toMillis(player.getServer().getPlayerSkinChangeCooldown()) > System.currentTimeMillis() - player.lastSkinChange);
        player.getServer().getEventManager().fire(playerChangeSkinEvent);
        if (!playerChangeSkinEvent.isCancelled()) {
            player.lastSkinChange = System.currentTimeMillis();
            player.setSkin(skin);
        }
        return true;
    }

    @Override
    public boolean handle(PlayerInputPacket packet) {
        if (!player.isAlive() || !player.spawned) {
            return true;
        }
        if (player.getVehicle() instanceof EntityAbstractMinecart) {
            ((EntityAbstractMinecart) player.getVehicle()).setCurrentSpeed(packet.getInputMotion().getY());
        }
        return true;
    }

    @Override
    public boolean handle(MovePlayerPacket packet) {
        if (player.getTeleportPosition() != null) {
            return true;
        }

        Vector3f newPos = packet.getPosition().sub(0, player.getEyeHeight(), 0);
        Vector3f currentPos = player.getPosition();

        float yaw = packet.getRotation().getY() % 360;
        float pitch = packet.getRotation().getX() % 360;

        if (yaw < 0) {
            yaw += 360;
        }

        if (newPos.distanceSquared(currentPos) < 0.01 && yaw == player.getYaw() && pitch == player.getPitch()) {
            return true;
        }

        if (currentPos.distance(newPos) > 50) {
            log.debug("packet too far REVERTING");
            player.sendPosition(currentPos, yaw, pitch, MovePlayerPacket.Mode.RESPAWN);
            return true;
        }

        boolean revert = false;
        if (!player.isAlive() || !player.spawned) {
            revert = true;
            player.setForceMovement(currentPos);
        }


        if (player.getForceMovement() != null && (newPos.distanceSquared(player.getForceMovement()) > 0.1 || revert)) {
            log.debug("packet forceMovement {} REVERTING {}", player.getForceMovement(), newPos);
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
        return true;
    }

    @Override
    public boolean handle(AdventureSettingsPacket packet) {
        Set<AdventureSetting> flags = packet.getSettings();
        if (!player.getServer().getAllowFlight() && flags.contains(AdventureSetting.FLYING) && !player.getAdventureSettings().get(org.cloudburstmc.api.player.AdventureSetting.ALLOW_FLIGHT)) {
            player.kick(PlayerKickEvent.Reason.FLYING_DISABLED, "Flying is not enabled on player server");
            return true;
        }
        PlayerToggleFlightEvent playerToggleFlightEvent = new PlayerToggleFlightEvent(player, flags.contains(AdventureSetting.FLYING));
        player.getServer().getEventManager().fire(playerToggleFlightEvent);
        if (playerToggleFlightEvent.isCancelled()) {
            player.getAdventureSettings().update();
        } else {
            player.getAdventureSettings().set(org.cloudburstmc.api.player.AdventureSetting.FLYING, playerToggleFlightEvent.isFlying());
        }
        return true;
    }

    @Override
    public boolean handle(EmotePacket packet) {
        for (CloudPlayer p : this.player.getViewers()) {
            p.sendPacket(packet);
        }
        return true;
    }

    @Override
    public boolean handle(PacketViolationWarningPacket packet) {
        log.warn("Recived Packet Violation Warning: {}", packet.toString());
        return true;
    }

    @Override
    public boolean handle(MobEquipmentPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return true;
        }

        boolean offhand = packet.getContainerId() == ContainerId.OFFHAND;
        ItemStack serverItem;
        if (offhand) {
            serverItem = player.getInventory().getOffHand();
        } else {
            serverItem = player.getInventory().getItem(packet.getHotbarSlot());
        }
        ItemStack clientItem = ItemUtils.fromNetwork(packet.getItem());

        if (!serverItem.equals(clientItem)) {
            log.debug("Tried to equip " + clientItem + " but have " + serverItem + " in target slot");
            player.getInventory().sendContents(player);
            return true;
        }
        if (offhand) {
            player.getInventory().setOffHandContents(clientItem);
        } else {
            player.getInventory().equipItem(packet.getHotbarSlot());
        }
        player.setUsingItem(false);

        return true;
    }

    @Override
    public boolean handle(PlayerActionPacket packet) {
        if (!player.spawned || (!player.isAlive() &&
                packet.getAction() != PlayerActionType.RESPAWN &&
                packet.getAction() != PlayerActionType.DIMENSION_CHANGE_REQUEST_OR_CREATIVE_DESTROY_BLOCK)) {
            return true;
        }

        packet.setRuntimeEntityId(player.getRuntimeId());
        Vector3f currentPos = player.getPosition();
        Vector3i blockPos = packet.getBlockPosition();
        Direction face = Direction.fromIndex(packet.getFace());

        switch (packet.getAction()) {
            case START_BREAK:
                long currentBreak = System.currentTimeMillis();
                Vector3i currentBreakPosition = packet.getBlockPosition();
                // HACK: Client spams multiple left clicks so we need to skip them.
                if ((lastBreakPosition.equals(currentBreakPosition) && (currentBreak - player.lastBreak) < 10) || currentPos.distanceSquared(blockPos.toFloat()) > 100) {
                    break;
                }
                Block target = player.getLevel().getBlock(blockPos);
                val targetState = target.getState();

                PlayerInteractEvent playerInteractEvent = new PlayerInteractEvent(player, player.getInventory().getItemInHand(), target, face, targetState == BlockStates.AIR ? PlayerInteractEvent.Action.LEFT_CLICK_AIR : PlayerInteractEvent.Action.LEFT_CLICK_BLOCK);
                player.getServer().getEventManager().fire(playerInteractEvent);
                if (playerInteractEvent.isCancelled()) {
                    player.getInventory().sendHeldItem(player);
                    break;
                }

                Block block = target.getSide(face);
                if (block.getState().getType() == BlockTypes.FIRE) {
                    block.set(BlockStates.AIR, true);
                    player.getLevel().addLevelSoundEvent(block.getPosition(), SoundEvent.EXTINGUISH_FIRE);
                    break;
                }
                if (!player.isCreative()) {
                    //improved player to take stuff like swimming, ladders, enchanted tools into account, fix wrong tool break time calculations for bad tools (pmmp/PocketMine-MP#211)
                    //Done by lmlstarqaq
                    double breakTime = Math.ceil(targetState.getBehavior().getBreakTime(targetState, player.getInventory().getItemInHand(), player) * 20);
                    if (breakTime > 0) {
                        LevelEventPacket levelEvent = new LevelEventPacket();
                        levelEvent.setType(LevelEventType.BLOCK_START_BREAK);
                        levelEvent.setPosition(blockPos.toFloat());
                        levelEvent.setData((int) (65535 / breakTime));
                        player.getLevel().addChunkPacket(blockPos, levelEvent);
                    }
                }

                player.breakingBlock = target;
                player.lastBreak = currentBreak;
                lastBreakPosition = currentBreakPosition;
                break;
            case ABORT_BREAK:
            case STOP_BREAK:
                LevelEventPacket levelEvent = new LevelEventPacket();
                levelEvent.setType(LevelEventType.BLOCK_STOP_BREAK);
                levelEvent.setPosition(blockPos.toFloat());
                levelEvent.setData(0);
                player.getLevel().addChunkPacket(blockPos, levelEvent);
                player.breakingBlock = null;
                break;
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

                player.getCraftingInventory().resetCraftingGrid();

                PlayerRespawnEvent playerRespawnEvent = new PlayerRespawnEvent(player, player.getSpawn());
                player.getServer().getEventManager().fire(playerRespawnEvent);

                Location respawnLoc = playerRespawnEvent.getRespawnLocation();

                player.teleport(respawnLoc, null);

                player.setSprinting(false);
                player.setSneaking(false);

                player.getData().setShort(EntityData.AIR_SUPPLY, 400);
                player.deadTicks = 0;
                player.noDamageTicks = 60;

                player.removeAllEffects();
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setLevel(20, 20);

                player.sendData(player);

                player.setMovementSpeed(DEFAULT_SPEED);

                player.getAdventureSettings().update();
                player.getInventory().sendContents(player);
                player.getInventory().sendArmorContents(player);

                player.spawnToAll();
                player.scheduleUpdate();
                break;
            case JUMP:
                player.getServer().getEventManager().fire(new PlayerJumpEvent(player));
                break;
            case START_SPRINT:
                PlayerToggleSprintEvent playerToggleSprintEvent = new PlayerToggleSprintEvent(player, true);
                player.getServer().getEventManager().fire(playerToggleSprintEvent);
                if (playerToggleSprintEvent.isCancelled()) {
                    player.sendFlags(player);
                } else {
                    player.setSprinting(true);
                }
                break;
            case STOP_SPRINT:
                playerToggleSprintEvent = new PlayerToggleSprintEvent(player, false);
                player.getServer().getEventManager().fire(playerToggleSprintEvent);
                if (playerToggleSprintEvent.isCancelled()) {
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
            case START_SNEAK:
                PlayerToggleSneakEvent playerToggleSneakEvent = new PlayerToggleSneakEvent(player, true);
                player.getServer().getEventManager().fire(playerToggleSneakEvent);
                if (playerToggleSneakEvent.isCancelled()) {
                    player.sendFlags(player);
                } else {
                    player.setSneaking(true);
                }
                break;
            case STOP_SNEAK:
                playerToggleSneakEvent = new PlayerToggleSneakEvent(player, false);
                player.getServer().getEventManager().fire(playerToggleSneakEvent);
                if (playerToggleSneakEvent.isCancelled()) {
                    player.sendFlags(player);
                } else {
                    player.setSneaking(false);
                }
                break;
            case DIMENSION_CHANGE_REQUEST_OR_CREATIVE_DESTROY_BLOCK:
                player.sendPosition(player.getPosition(), player.getYaw(), player.getPitch(), MovePlayerPacket.Mode.NORMAL);
                break; //TODO
            case START_GLIDE:
                PlayerToggleGlideEvent playerToggleGlideEvent = new PlayerToggleGlideEvent(player, true);
                player.getServer().getEventManager().fire(playerToggleGlideEvent);
                if (playerToggleGlideEvent.isCancelled()) {
                    player.sendFlags(player);
                } else {
                    player.setGliding(true);
                }
                break;
            case STOP_GLIDE:
                playerToggleGlideEvent = new PlayerToggleGlideEvent(player, false);
                player.getServer().getEventManager().fire(playerToggleGlideEvent);
                if (playerToggleGlideEvent.isCancelled()) {
                    player.sendFlags(player);
                } else {
                    player.setGliding(false);
                }
                break;
            case CONTINUE_BREAK:
                if (player.isBreakingBlock()) {
                    block = player.getLevel().getBlock(blockPos);
                    player.getLevel().addParticle(new PunchBlockParticle(blockPos.toFloat(), block.getState(), face));
                }
                break;
            case START_SWIMMING:
                PlayerToggleSwimEvent ptse = new PlayerToggleSwimEvent(player, true);
                player.getServer().getEventManager().fire(ptse);

                if (ptse.isCancelled()) {
                    player.sendFlags(player);
                } else {
                    player.setSwimming(true);
                }
                break;
            case STOP_SWIMMING:
                ptse = new PlayerToggleSwimEvent(player, false);
                player.getServer().getEventManager().fire(ptse);

                if (ptse.isCancelled()) {
                    player.sendFlags(player);
                } else {
                    player.setSwimming(false);
                }
                break;
        }
        player.getData().update();

        player.setUsingItem(false);
        return true;
    }

    @Override
    public boolean handle(ModalFormResponsePacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return true;
        }

        Form<?> window = player.removeFormWindow(packet.getFormId());

        if (window == null) {
            if (player.getServerSettings() != null && player.getServerSettingsId() == packet.getFormId()) {
                window = player.getServerSettings();
            } else {
                return true;
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
        } catch (JsonProcessingException e) {
            log.debug("Received corrupted form json data");
        }
        return true;
    }

    @Override
    public boolean handle(InteractPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return true;
        }

        player.getCraftingInventory().resetCraftingGrid();

        Entity targetEntity = player.getLevel().getEntity(packet.getRuntimeEntityId());

        if (targetEntity == null || !player.isAlive() || !targetEntity.isAlive()) {
            return true;
        }

        if (targetEntity instanceof DroppedItem || targetEntity instanceof EntityArrow || targetEntity instanceof ExperienceOrb) {
            player.kick(PlayerKickEvent.Reason.INVALID_PVE, "Attempting to interact with an invalid entity");
            log.warn(player.getServer().getLanguage().translate("cloudburst.player.invalidEntity", player.getName()));
            return true;
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
                    player.getInventory().open(player);
                    player.openInventory(player);
                }
                break;
        }
        return true;
    }

    @Override
    public boolean handle(BlockPickRequestPacket packet) {
        Vector3i pickPos = packet.getBlockPosition();
        Block block = player.getLevel().getBlock(pickPos.getX(), pickPos.getY(), pickPos.getZ());
        ItemStack serverItem = block.getState().getBehavior().toItem(block);

        if (packet.isAddUserData()) {
            BaseBlockEntity blockEntity = (BaseBlockEntity) player.getLevel().getLoadedBlockEntity(
                    Vector3i.from(pickPos.getX(), pickPos.getY(), pickPos.getZ()));
            if (blockEntity != null) {
                NbtMap nbt = blockEntity.getItemTag();
                if (nbt != null) {
//                    CloudItemStackBuilder builder = (CloudItemStackBuilder) serverItem.toBuilder(); //TODO
//                    builder.nbt()
//                    serverItem.addTag(nbt);
//                    serverItem.setLore("+(DATA)");
                }
            }
        }

        PlayerBlockPickEvent pickEvent = new PlayerBlockPickEvent(player, block, serverItem);
        if (player.isSpectator()) {
            log.debug("Got block-pick request from " + player.getName() + " when in spectator mode");
            pickEvent.setCancelled();
        }

        player.getServer().getEventManager().fire(pickEvent);

        if (!pickEvent.isCancelled()) {
            boolean itemExists = false;
            int itemSlot = -1;
            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                if (player.getInventory().getItem(slot).equals(pickEvent.getItem())) {
                    if (slot < player.getInventory().getHotbarSize()) {
                        player.getInventory().setHeldItemSlot(slot);
                    } else {
                        itemSlot = slot;
                    }
                    itemExists = true;
                    break;
                }
            }

            for (int slot = 0; slot < player.getInventory().getHotbarSize(); slot++) {
                if (player.getInventory().getItem(slot).isNull()) {
                    if (!itemExists && player.isCreative()) {
                        player.getInventory().setHeldItemSlot(slot);
                        player.getInventory().setItemInHand(pickEvent.getItem());
                        return true;
                    } else if (itemSlot > -1) {
                        player.getInventory().setHeldItemSlot(slot);
                        player.getInventory().setItemInHand(player.getInventory().getItem(itemSlot));
                        player.getInventory().clear(itemSlot, true);
                        return true;
                    }
                }
            }

            if (!itemExists && player.isCreative()) {
                ItemStack itemInHand = player.getInventory().getItemInHand();
                player.getInventory().setItemInHand(pickEvent.getItem());
                if (!player.getInventory().isFull()) {
                    for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                        if (player.getInventory().getItem(slot).isNull()) {
                            player.getInventory().setItem(slot, itemInHand);
                            break;
                        }
                    }
                }
            } else if (itemSlot > -1) {
                ItemStack itemInHand = player.getInventory().getItemInHand();
                player.getInventory().setItemInHand(player.getInventory().getItem(itemSlot));
                player.getInventory().setItem(itemSlot, itemInHand);
            }
        }
        return true;
    }

    @Override
    public boolean handle(AnimatePacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return true;
        }

        PlayerAnimationEvent animationEvent = new PlayerAnimationEvent(player, PlayerAnimationEvent.Type.values()[packet.getAction().ordinal()]);
        player.getServer().getEventManager().fire(animationEvent);
        if (animationEvent.isCancelled()) {
            return true;
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
        return true;
    }

    @Override
    public boolean handle(SetHealthPacket packet) {
        // Cannot be trusted. Use UpdateAttributePacket instead
        return true;
    }

    @Override
    public boolean handle(EntityEventPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return true;
        }

        player.getCraftingInventory().resetCraftingGrid();

        if (packet.getType() == EntityEventType.EATING_ITEM) {
            if (packet.getData() == 0 || packet.getRuntimeEntityId() != player.getRuntimeId()) {
                return true;
            }

            packet.setRuntimeEntityId(player.getRuntimeId());

            player.sendPacket(packet);
            CloudServer.broadcastPacket(player.getViewers(), packet);
        }
        return true;
    }

    @Override
    public boolean handle(CommandRequestPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return true;
        }
        player.getCraftingInventory().setCraftingGridType(CraftingGrid.Type.CRAFTING_GRID_SMALL);
        PlayerCommandPreprocessEvent playerCommandPreprocessEvent = new PlayerCommandPreprocessEvent(player, packet.getCommand());
        player.getServer().getEventManager().fire(playerCommandPreprocessEvent);
        if (playerCommandPreprocessEvent.isCancelled()) {
            return true;
        }

        try (Timing ignored2 = Timings.playerCommandTimer.startTiming()) {
            player.getServer().dispatchCommand((CommandSender) playerCommandPreprocessEvent.getPlayer(), playerCommandPreprocessEvent.getMessage().substring(1));
        }
        return true;
    }

    @Override
    public boolean handle(TextPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return true;
        }

        if (packet.getType() == TextPacket.Type.CHAT) {
            player.chat(packet.getMessage());
        }
        return true;
    }

    @Override
    public boolean handle(ContainerClosePacket packet) {
        if (!player.spawned || packet.getId() == ContainerId.INVENTORY && player.canOpenInventory()) {
            return true;
        }

        if (player.getWindowById(packet.getId()) != null) {
            player.getServer().getEventManager().fire(new InventoryCloseEvent(player.getWindowById(packet.getId()), player));
            if (packet.getId() == ContainerId.INVENTORY) player.closeInventory(player);
            player.removeWindow(player.getWindowById(packet.getId()));
        }

        if (packet.getId() == -1) {
            player.getCraftingInventory().resetCraftingGrid();

            ContainerClosePacket ccp = new ContainerClosePacket();
            ccp.setId((byte) -1);
            player.sendPacket(ccp);
        }
        return true;
    }

    @Override
    public boolean handle(CraftingEventPacket packet) {
        return true;
    }

    @Override
    public boolean handle(BlockEntityDataPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return true;
        }

        player.getCraftingInventory().resetCraftingGrid();

        Vector3i blockPos = packet.getBlockPosition();
        if (blockPos.distanceSquared(player.getPosition().toInt()) > 10000) {
            return true;
        }

        BaseBlockEntity blockEntity = (BaseBlockEntity) player.getLevel().getLoadedBlockEntity(blockPos);
        if (blockEntity != null && blockEntity.isSpawnable()) {
            if (!blockEntity.updateFromClient(packet.getData(), player)) {
                blockEntity.spawnTo(player);
            }
        }
        return true;
    }

    @Override
    public boolean handle(RequestChunkRadiusPacket packet) {
        player.getChunkManager().setChunkRadius(packet.getRadius());
        return true;
    }

    @Override
    public boolean handle(SetPlayerGameTypePacket packet) {
        if (packet.getGamemode() != player.getGamemode().getVanillaId()) {
            if (!player.hasPermission("cloudburst.command.gamemode")) {
                SetPlayerGameTypePacket packet1 = new SetPlayerGameTypePacket();
                packet1.setGamemode(player.getGamemode().getVanillaId());
                player.sendPacket(packet1);
                player.getAdventureSettings().update();
                return true;
            }
            player.setGamemode(GameMode.from(packet.getGamemode()), true);
            CommandUtils.broadcastCommandMessage(player, new TranslationContainer("%commands.gamemode.success.self", player.getGamemode().getTranslation()));
        }
        return true;
    }

    @Override
    public boolean handle(ItemFrameDropItemPacket packet) {
        Vector3i vector3 = packet.getBlockPosition();
        BlockEntity blockEntity = player.getLevel().getLoadedBlockEntity(vector3);
        if (!(blockEntity instanceof ItemFrame)) {
            return true;
        }
        ItemFrame itemFrame = (ItemFrame) blockEntity;
        Block block = itemFrame.getBlock();
        ItemStack itemDrop = itemFrame.getItem();
        ItemFrameDropItemEvent itemFrameDropItemEvent = new ItemFrameDropItemEvent(player, block, itemFrame, itemDrop);
        player.getServer().getEventManager().fire(itemFrameDropItemEvent);
        if (!itemFrameDropItemEvent.isCancelled()) {
            if (itemDrop.getType() != AIR) {
                player.getLevel().dropItem(itemFrame.getPosition(), itemDrop);
                itemFrame.setItem(CloudItemRegistry.AIR);
                itemFrame.setItemRotation(0);
                player.getLevel().addSound(player.getPosition(), Sound.BLOCK_ITEMFRAME_REMOVE_ITEM);
            }
        } else {
            itemFrame.spawnTo(player);
        }
        return true;
    }

    @Override
    public boolean handle(MapInfoRequestPacket packet) {
        ItemStack mapItem = null;

        for (ItemStack item1 : player.getInventory().getContents().values()) {
            if (item1.getType() != ItemTypes.MAP) {
                continue;
            }

            val data = item1.getMetadata(MapItem.class);

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
                    val frameItem = itemFrame1.getItem();

                    if (frameItem.getType() != ItemTypes.MAP) {
                        continue;
                    }

                    val data = frameItem.getMetadata(MapItem.class);

                    if (data == null) {
                        continue;
                    }

                    if (data.getId() == packet.getUniqueMapId()) {
//                        ((ItemMapBehavior) itemFrame1.getItem()).sendImage(player); //TODO: send image
                        break;
                    }
                }
            }

            return true;
        }

        PlayerMapInfoRequestEvent event;
        player.getServer().getEventManager().fire(event = new PlayerMapInfoRequestEvent(player, mapItem));

        if (!event.isCancelled()) {
//                ((ItemMapBehavior) mapItem).sendImage(player); //TODO: send image
        }
        return true;
    }

    @Override
    public boolean handle(LevelSoundEvent2Packet packet) {
        if (!player.isSpectator() || (packet.getSound() != SoundEvent.HIT &&
                packet.getSound() != SoundEvent.ATTACK_NODAMAGE)) {
            player.getLevel().addChunkPacket(player.getPosition(), packet);
        }
        return true;
    }

    @Override
    public boolean handle(InventoryTransactionPacket packet) {
        if (player.isSpectator()) {
            player.sendAllInventories();
            return true;
        }

        List<InventoryAction> actions = new ArrayList<>();
        for (InventoryActionData inventoryActionData : packet.getActions()) {
            InventoryAction a = InventoryTransactionUtils.createInventoryAction(player, inventoryActionData);

            if (a == null) {
                log.debug("Unmatched inventory action from " + player.getName() + ": " + inventoryActionData);
                player.sendAllInventories();
                return true;
            }

            actions.add(a);
        }

        if (InventoryTransactionUtils.containsCraftingPart(packet)) {
            if (player.getCraftingTransaction() == null) {
                player.setCraftingTransaction(new CraftingTransaction(player, actions));
            } else {
                for (InventoryAction action : actions) {
                    player.getCraftingTransaction().addAction(action);
                }
            }

            if (player.getCraftingTransaction().getPrimaryOutput() != null) {
                //we get the actions for player in several packets, so we can't execute it until we get the result

                player.getCraftingTransaction().execute();
                player.setCraftingTransaction(null);
            }

            return true;
        } else if (player.getCraftingTransaction() != null) {
            log.debug("Got unexpected normal inventory action with incomplete crafting transaction from " + player.getName() + ", refusing to execute crafting");
            player.setCraftingTransaction(null);
        }

        switch (packet.getTransactionType()) {
            case NORMAL:
                InventoryTransaction transaction = new InventoryTransaction(player, actions);

                if (!transaction.execute()) {
                    log.debug("Failed to execute inventory transaction from " + player.getName() + " with actions: " + packet.getActions());
                    return true;
                }
                //TODO: fix achievement for getting iron from furnace
                return true;
            case INVENTORY_MISMATCH:
                if (packet.getActions().size() > 0) {
                    log.debug("Expected 0 actions for mismatch, got " + packet.getActions().size() + ", " + packet.getActions());
                }
                player.sendAllInventories();

                return true;
            case ITEM_USE:

                Vector3i blockVector = packet.getBlockPosition();
                Direction face = Direction.fromIndex(packet.getBlockFace());

                switch (packet.getActionType()) {
                    case InventoryTransactionUtils.USE_ITEM_ACTION_CLICK_BLOCK:
                        // Remove if client bug is ever fixed
                        boolean spamBug = (lastRightClickPos != null && System.currentTimeMillis() - lastRightClickTime < 100.0 && blockVector.distanceSquared(lastRightClickPos) < 0.00001);
                        lastRightClickPos = blockVector;
                        lastRightClickTime = System.currentTimeMillis();
                        if (spamBug) {
                            return true;
                        }

                        player.setUsingItem(false);

                        if (player.canInteract(blockVector.toFloat().add(0.5, 0.5, 0.5), player.isCreative() ? 13 : 7)) {
                            ItemStack clientHand = ItemUtils.fromNetwork(packet.getItemInHand());
                            if (player.isCreative()) {
                                ItemStack i = player.getInventory().getItemInHand();
                                if (player.getLevel().useItemOn(blockVector, i, face,
                                        packet.getClickPosition(), player) != null) {
                                    return true;
                                }
                            } else if (player.getInventory().getItemInHand().equals(clientHand)) {
                                ItemStack serverHand = player.getInventory().getItemInHand();
                                ItemStack oldItem = serverHand;
                                //TODO: Implement adventure mode checks
                                if ((serverHand = player.getLevel().useItemOn(blockVector, serverHand, face,
                                        packet.getClickPosition(), player)) != null) {
                                    if (!serverHand.equals(oldItem) ||
                                            serverHand.getAmount() != oldItem.getAmount()) {
                                        player.getInventory().setItemInHand(serverHand);
                                        player.getInventory().sendHeldItem(player.getViewers());
                                    }
                                    return true;
                                }
                            }
                        }

                        player.getInventory().sendHeldItem(player);

                        if (blockVector.distanceSquared(player.getPosition().toInt()) > 10000) {
                            return true;
                        }

                        Block target = player.getLevel().getBlock(blockVector);
                        Block blockState = target.getSide(face);

                        player.getLevel().sendBlocks(new CloudPlayer[]{player}, new Block[]{target, blockState}, UpdateBlockPacket.FLAG_ALL_PRIORITY);
                        return true;
                    case InventoryTransactionUtils.USE_ITEM_ACTION_BREAK_BLOCK:
                        if (!player.spawned || !player.isAlive()) {
                            return true;
                        }

                        player.getCraftingInventory().resetCraftingGrid();

                        ItemStack i = player.getInventory().getItemInHand();

                        ItemStack oldItem = i;

                        if (player.canInteract(blockVector.toFloat().add(0.5, 0.5, 0.5), player.isCreative() ? 13 : 7) &&
                                (i = player.getLevel().useBreakOn(blockVector, face, i, player, true)) != null) {
                            if (player.isSurvival()) {
                                player.getFoodData().updateFoodExpLevel(0.025);
                                if (!i.equals(oldItem) || i.getAmount() != oldItem.getAmount()) {
                                    player.getInventory().setItemInHand(i);
                                    player.getInventory().sendHeldItem(player.getViewers());
                                }
                            }
                            return true;
                        }

                        player.getInventory().sendContents(player);
                        target = player.getLevel().getBlock(blockVector);
                        BlockEntity blockEntity = player.getLevel().getLoadedBlockEntity(blockVector);

                        player.getLevel().sendBlocks(new CloudPlayer[]{player}, new Block[]{target}, UpdateBlockPacket.FLAG_ALL_PRIORITY);

                        player.getInventory().sendHeldItem(player);

                        if (blockEntity != null && blockEntity.isSpawnable()) {
                            blockEntity.spawnTo(player);
                        }

                        return true;
                    case InventoryTransactionUtils.USE_ITEM_ACTION_CLICK_AIR:
                        Vector3f directionVector = player.getDirectionVector();

                        ItemStack clientHand = ItemUtils.fromNetwork(packet.getItemInHand());
                        ItemStack serverItem;

                        if (player.isCreative()) {
                            serverItem = player.getInventory().getItemInHand();
                        } else if (!player.getInventory().getItemInHand().equals(clientHand)) {
                            player.getInventory().sendHeldItem(player);
                            return true;
                        } else {
                            serverItem = player.getInventory().getItemInHand();
                        }

                        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, serverItem, directionVector, face, PlayerInteractEvent.Action.RIGHT_CLICK_AIR);

                        player.getServer().getEventManager().fire(interactEvent);

                        if (interactEvent.isCancelled()) {
                            player.getInventory().sendHeldItem(player);
                            return true;
                        }

                        if (serverItem.getBehavior().onClickAir(serverItem, directionVector, player)) {
                            if (player.getGamemode().isSurvival()) {
                                player.getInventory().setItemInHand(serverItem);
                            }

                            if (!player.isUsingItem()) {
                                player.setUsingItem(true);
                                return true;
                            }

                            // Used item
                            int ticksUsed = player.getServer().getTick() - player.getStartActionTick();
                            player.setUsingItem(false);

                            val result = serverItem.getBehavior().onUse(serverItem, ticksUsed, player);
                            if (result != null) {
                                player.getInventory().setItemInHand(result);
                            } else {
                                player.getInventory().sendContents(player);
                            }
                        }

                        return true;
                    default:
                        //unknown
                        break;
                }
                break;
            case ITEM_USE_ON_ENTITY:

                Entity target = player.getLevel().getEntity(packet.getRuntimeEntityId());
                if (target == null) {
                    return true;
                }

                ItemStack clientHand = ItemUtils.fromNetwork(packet.getItemInHand());

                if (!clientHand.equals(player.getInventory().getItemInHand(), true)) {
                    player.getInventory().sendHeldItem(player);
                }

                ItemStack serverItem = player.getInventory().getItemInHand();

                switch (packet.getActionType()) {
                    case InventoryTransactionUtils.USE_ITEM_ON_ENTITY_ACTION_INTERACT:
                        PlayerInteractEntityEvent playerInteractEntityEvent = new PlayerInteractEntityEvent(player, target, serverItem, packet.getClickPosition());
                        if (player.isSpectator()) playerInteractEntityEvent.setCancelled();
                        player.getServer().getEventManager().fire(playerInteractEntityEvent);

                        if (playerInteractEntityEvent.isCancelled()) {
                            break;
                        }
                        if (target.onInteract(player, serverItem, packet.getClickPosition()) && player.isSurvival()) {
                            val behavior = serverItem.getBehavior();
                            val result = behavior.useOn(serverItem, target);
                            if (result == null) {
                                if (serverItem.getAmount() > 1) {
                                    serverItem = serverItem.decrementAmount();
                                } else {
                                    serverItem = CloudItemRegistry.AIR;
                                }
                            } else {
                                serverItem = result;
                            }

                            player.getInventory().setItemInHand(serverItem);
                        }
                        break;
                    case InventoryTransactionUtils.USE_ITEM_ON_ENTITY_ACTION_ATTACK:
                        val behavior = serverItem.getBehavior();
                        float itemDamage = behavior.getAttackDamage(serverItem);

                        for (EnchantmentInstance enchantment : serverItem.getEnchantments().values()) {
                            itemDamage += enchantment.getBehavior().getDamageBonus(enchantment, target);
                        }

                        Map<EntityDamageEvent.DamageModifier, Float> damage = new EnumMap<>(EntityDamageEvent.DamageModifier.class);
                        damage.put(EntityDamageEvent.DamageModifier.BASE, itemDamage);

                        if (!player.canInteract(target.getPosition(), player.isCreative() ? 8 : 5)) {
                            break;
                        } else if (target instanceof CloudPlayer) {
                            if (((CloudPlayer) target).getGamemode() != GameMode.SURVIVAL) {
                                break;
                            } else if (!player.getServer().getConfig().isPVP()) {
                                break;
                            }
                        }

                        int knockback = 0;

                        EnchantmentInstance enchKnockback = player.getInventory().getItemInHand().getEnchantment(EnchantmentTypes.KNOCKBACK);
                        if (enchKnockback != null) {
                            knockback = enchKnockback.getLevel();
                        }

                        EntityDamageByEntityEvent entityDamageByEntityEvent = new EntityDamageByEntityEvent(player, target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage);
                        if (player.isSpectator()) entityDamageByEntityEvent.setCancelled();
                        if ((target instanceof CloudPlayer) && !player.getLevel().getGameRules().get(GameRules.PVP)) {
                            entityDamageByEntityEvent.setCancelled();
                        }

                        if (!target.attack(entityDamageByEntityEvent)) {
                            if (behavior.isTool(serverItem) && player.isSurvival()) {
                                player.getInventory().sendContents(player);
                            }
                            break;
                        }

                        if (!entityDamageByEntityEvent.isCancelled()) {
                            if (knockback > 0) {
                                if (target instanceof EntityLiving) {
                                    Vector3f diff = target.getPosition().sub(player.getPosition());
                                    ((EntityLiving) target).knockBack(player, knockback * 2, diff.getX(), diff.getZ());
                                }

                                player.setSprinting(false);
                            }

                            for (EnchantmentInstance enchantment : serverItem.getEnchantments().values()) {
                                enchantment.getBehavior().doPostAttack(enchantment, player, target);
                            }
                        }

                        if (behavior.isTool(serverItem) && player.isSurvival()) {
                            val result = behavior.useOn(serverItem, target);
                            if (result == null && serverItem.getMetadata(Damageable.class).getDurability() >= behavior.getMaxDurability()) {
                                player.getInventory().setItemInHand(CloudItemRegistry.AIR);
                            } else {
                                player.getInventory().setItemInHand(result);
                            }
                        }
                        return true;
                    default:
                        break; //unknown
                }

                break;
            case ITEM_RELEASE:
                if (player.isSpectator()) {
                    player.sendAllInventories();
                    return true;
                }

                try {
                    switch (packet.getActionType()) {
                        case InventoryTransactionUtils.RELEASE_ITEM_ACTION_RELEASE:
                            if (player.isUsingItem()) {
                                serverItem = player.getInventory().getItemInHand();

                                int ticksUsed = player.getServer().getTick() - player.getStartActionTick();
                                val result = serverItem.getBehavior().onRelease(serverItem, ticksUsed, player);
                                if (result != null) {
                                    player.getInventory().setItemInHand(result);
                                }

                                player.setUsingItem(false);
                            } else {
                                player.getInventory().sendContents(player);
                            }
                            return true;
                        case InventoryTransactionUtils.RELEASE_ITEM_ACTION_CONSUME:
                            log.debug("Unexpected release item action consume from {}", player::getName);
                            return true;
                        default:
                            break;
                    }
                } finally {
                    player.setUsingItem(false);
                }
                break;
            default:
                player.getInventory().sendContents(player);
                break;
        }
        return true;
    }

    @Override
    public boolean handle(PlayerHotbarPacket packet) {
        if (packet.getContainerId() != ContainerId.INVENTORY) {
            return true; // This should never happen
        }

        player.getInventory().equipItem(packet.getSelectedHotbarSlot());
        return true;
    }

    @Override
    public boolean handle(ServerSettingsRequestPacket packet) {
        CustomForm settings = player.getServerSettings();

        if (settings == null) {
            return true;
        }

        try {
            ServerSettingsResponsePacket re = new ServerSettingsResponsePacket();
            re.setFormId(player.getServerSettingsId());
            re.setFormData(new JsonMapper().writeValueAsString(settings));
            player.sendPacket(re);
        } catch (JsonProcessingException e) {
            log.error("Error while writing form data", e);
        }
        return true;
    }

    @Override
    public boolean handle(RespawnPacket packet) {
        if (player.isAlive()) {
            return true;
        }
        if (packet.getState() == RespawnPacket.State.CLIENT_READY) {
            RespawnPacket respawn1 = new RespawnPacket();
            respawn1.setPosition(player.getSpawn().getPosition());
            respawn1.setState(RespawnPacket.State.SERVER_READY);
            player.sendPacket(respawn1);
        }
        return true;
    }

    @Override
    public boolean handle(LecternUpdatePacket packet) {
        Vector3i blockPosition = packet.getBlockPosition();

        if (packet.isDroppingBook()) {
            Block block = player.getLevel().getBlock(blockPosition);
            val state = block.getState();
            if (state.getType() == BlockTypes.LECTERN) {
                ((BlockBehaviorLectern) state.getBehavior()).dropBook(block, player);
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
                    val block = lectern.getBlock();
                    val state = block.getState();
                    if (state.getType() == BlockTypes.LECTERN) {
                        ((BlockBehaviorLectern) state.getBehavior()).executeRedstonePulse(block);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean handle(SetLocalPlayerAsInitializedPacket packet) {
        if (player.isInitialized()) {
            return true;
        }
        player.setInitialized(true);
        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(player,
                new TranslationContainer(TextFormat.YELLOW + "%multiplayer.player.joined", player.getDisplayName())
        );

        player.getServer().getEventManager().fire(playerJoinEvent);

        if (playerJoinEvent.getJoinMessage().toString().trim().length() > 0) {
            player.getServer().broadcastMessage(playerJoinEvent.getJoinMessage());
        }
        return true;
    }
}
