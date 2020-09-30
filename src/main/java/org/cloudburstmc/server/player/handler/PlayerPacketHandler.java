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
import org.cloudburstmc.server.AdventureSettings;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.behavior.BlockBehaviorLectern;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.ItemFrame;
import org.cloudburstmc.server.blockentity.Lectern;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.impl.EntityLiving;
import org.cloudburstmc.server.entity.impl.projectile.EntityArrow;
import org.cloudburstmc.server.entity.impl.vehicle.EntityAbstractMinecart;
import org.cloudburstmc.server.entity.impl.vehicle.EntityBoat;
import org.cloudburstmc.server.entity.misc.DroppedItem;
import org.cloudburstmc.server.entity.misc.ExperienceOrb;
import org.cloudburstmc.server.event.block.ItemFrameDropItemEvent;
import org.cloudburstmc.server.event.block.LecternPageChangeEvent;
import org.cloudburstmc.server.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.event.inventory.InventoryCloseEvent;
import org.cloudburstmc.server.event.player.*;
import org.cloudburstmc.server.event.server.DataPacketReceiveEvent;
import org.cloudburstmc.server.form.CustomForm;
import org.cloudburstmc.server.form.Form;
import org.cloudburstmc.server.inventory.transaction.CraftingTransaction;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.inventory.transaction.action.InventoryAction;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemMap;
import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.gamerule.GameRules;
import org.cloudburstmc.server.level.particle.PunchBlockParticle;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.network.protocol.types.InventoryTransactionUtils;
import org.cloudburstmc.server.player.GameMode;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.TextFormat;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.cloudburstmc.server.block.BlockIds.AIR;
import static org.cloudburstmc.server.player.Player.CraftingType;
import static org.cloudburstmc.server.player.Player.DEFAULT_SPEED;

/**
 * @author Extollite
 */
@Log4j2
public class PlayerPacketHandler implements BedrockPacketHandler {
    private final Player player;

    protected Vector3i lastRightClickPos = null;
    protected double lastRightClickTime = 0.0;

    private Vector3i lastBreakPosition = Vector3i.ZERO;

    public PlayerPacketHandler(Player player) {
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

        PlayerChangeSkinEvent playerChangeSkinEvent = new PlayerChangeSkinEvent(player, skin);
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
        if (!player.getServer().getAllowFlight() && flags.contains(AdventureSetting.FLYING) && !player.getAdventureSettings().get(AdventureSettings.Type.ALLOW_FLIGHT)) {
            player.kick(PlayerKickEvent.Reason.FLYING_DISABLED, "Flying is not enabled on player server");
            return true;
        }
        PlayerToggleFlightEvent playerToggleFlightEvent = new PlayerToggleFlightEvent(player, flags.contains(AdventureSetting.FLYING));
        player.getServer().getEventManager().fire(playerToggleFlightEvent);
        if (playerToggleFlightEvent.isCancelled()) {
            player.getAdventureSettings().update();
        } else {
            player.getAdventureSettings().set(AdventureSettings.Type.FLYING, playerToggleFlightEvent.isFlying());
        }
        return true;
    }

    @Override
    public boolean handle(EmotePacket packet) {
        for (Player p : this.player.getViewers()) {
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
        Item serverItem;
        if (offhand) {
            serverItem = player.getInventory().getOffHand();
        } else {
            serverItem = player.getInventory().getItem(packet.getHotbarSlot());
        }
        Item clientItem = Item.fromNetwork(packet.getItem());

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
                packet.getAction() != PlayerActionPacket.Action.RESPAWN &&
                packet.getAction() != PlayerActionPacket.Action.DIMENSION_CHANGE_REQUEST)) {
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
                if (block.getState().getType() == BlockIds.FIRE) {
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

                player.craftingType = CraftingType.SMALL;
                player.resetCraftingGridType();

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
            case DIMENSION_CHANGE_REQUEST:
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

        player.craftingType = CraftingType.SMALL;
        //this.resetCraftingGridType();

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
        Item serverItem = block.getState().getBehavior().toItem(block);

        if (packet.isAddUserData()) {
            BlockEntity blockEntity = player.getLevel().getLoadedBlockEntity(
                    Vector3i.from(pickPos.getX(), pickPos.getY(), pickPos.getZ()));
            if (blockEntity != null) {
                NbtMap nbt = blockEntity.getItemTag();
                if (nbt != null) {
                    serverItem.addTag(nbt);
                    serverItem.setLore("+(DATA)");
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
                Item itemInHand = player.getInventory().getItemInHand();
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
                Item itemInHand = player.getInventory().getItemInHand();
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

        PlayerAnimationEvent animationEvent = new PlayerAnimationEvent(player, packet.getAction());
        player.getServer().getEventManager().fire(animationEvent);
        if (animationEvent.isCancelled()) {
            return true;
        }

        AnimatePacket.Action animation = animationEvent.getAnimationType();

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
        animatePacket.setAction(animationEvent.getAnimationType());
        Server.broadcastPacket(player.getViewers(), animatePacket);
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
        player.craftingType = CraftingType.SMALL;
        //player.resetCraftingGridType();

        if (packet.getType() == EntityEventType.EATING_ITEM) {
            if (packet.getData() == 0 || packet.getRuntimeEntityId() != player.getRuntimeId()) {
                return true;
            }

            packet.setRuntimeEntityId(player.getRuntimeId());

            player.sendPacket(packet);
            Server.broadcastPacket(player.getViewers(), packet);
        }
        return true;
    }

    @Override
    public boolean handle(CommandRequestPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return true;
        }
        player.craftingType = CraftingType.SMALL;
        PlayerCommandPreprocessEvent playerCommandPreprocessEvent = new PlayerCommandPreprocessEvent(player, packet.getCommand());
        player.getServer().getEventManager().fire(playerCommandPreprocessEvent);
        if (playerCommandPreprocessEvent.isCancelled()) {
            return true;
        }

        try (Timing ignored2 = Timings.playerCommandTimer.startTiming()) {
            player.getServer().dispatchCommand(playerCommandPreprocessEvent.getPlayer(), playerCommandPreprocessEvent.getMessage().substring(1));
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
            player.craftingType = CraftingType.SMALL;
            player.resetCraftingGridType();
            player.addWindow(player.getCraftingGrid(), (byte) ContainerId.NONE);
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

        player.craftingType = CraftingType.SMALL;
        player.resetCraftingGridType();

        Vector3i blockPos = packet.getBlockPosition();
        if (blockPos.distanceSquared(player.getPosition().toInt()) > 10000) {
            return true;
        }

        BlockEntity blockEntity = player.getLevel().getLoadedBlockEntity(blockPos);
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
        Item itemDrop = itemFrame.getItem();
        ItemFrameDropItemEvent itemFrameDropItemEvent = new ItemFrameDropItemEvent(player, block, itemFrame, itemDrop);
        player.getServer().getEventManager().fire(itemFrameDropItemEvent);
        if (!itemFrameDropItemEvent.isCancelled()) {
            if (itemDrop.getId() != AIR) {
                player.getLevel().dropItem(itemFrame.getPosition(), itemDrop);
                itemFrame.setItem(Item.get(AIR, 0, 0));
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
        Item mapItem = null;

        for (Item item1 : player.getInventory().getContents().values()) {
            if (item1 instanceof ItemMap && ((ItemMap) item1).getMapId() == packet.getUniqueMapId()) {
                mapItem = item1;
            }
        }

        if (mapItem == null) {
            for (BlockEntity be : player.getLevel().getBlockEntities()) {
                if (be instanceof ItemFrame) {
                    ItemFrame itemFrame1 = (ItemFrame) be;

                    if (itemFrame1.getItem() instanceof ItemMap && ((ItemMap) itemFrame1.getItem()).getMapId() == packet.getUniqueMapId()) {
                        ((ItemMap) itemFrame1.getItem()).sendImage(player);
                        break;
                    }
                }
            }
        }

        if (mapItem != null) {
            PlayerMapInfoRequestEvent event;
            player.getServer().getEventManager().fire(event = new PlayerMapInfoRequestEvent(player, mapItem));

            if (!event.isCancelled()) {
                ((ItemMap) mapItem).sendImage(player);
            }
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
                            Item clientHand = Item.fromNetwork(packet.getItemInHand());
                            if (player.isCreative()) {
                                Item i = player.getInventory().getItemInHand();
                                if (player.getLevel().useItemOn(blockVector, i, face,
                                        packet.getClickPosition(), player) != null) {
                                    return true;
                                }
                            } else if (player.getInventory().getItemInHand().equals(clientHand)) {
                                Item serverHand = player.getInventory().getItemInHand();
                                Item oldItem = serverHand.clone();
                                //TODO: Implement adventure mode checks
                                if ((serverHand = player.getLevel().useItemOn(blockVector, serverHand, face,
                                        packet.getClickPosition(), player)) != null) {
                                    if (!serverHand.equals(oldItem) ||
                                            serverHand.getCount() != oldItem.getCount()) {
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

                        player.getLevel().sendBlocks(new Player[]{player}, new Block[]{target, blockState}, UpdateBlockPacket.FLAG_ALL_PRIORITY);
                        return true;
                    case InventoryTransactionUtils.USE_ITEM_ACTION_BREAK_BLOCK:
                        if (!player.spawned || !player.isAlive()) {
                            return true;
                        }

                        player.resetCraftingGridType();

                        Item i = player.getInventory().getItemInHand();

                        Item oldItem = i.clone();

                        if (player.canInteract(blockVector.toFloat().add(0.5, 0.5, 0.5), player.isCreative() ? 13 : 7) &&
                                (i = player.getLevel().useBreakOn(blockVector, face, i, player, true)) != null) {
                            if (player.isSurvival()) {
                                player.getFoodData().updateFoodExpLevel(0.025);
                                if (!i.equals(oldItem) || i.getCount() != oldItem.getCount()) {
                                    player.getInventory().setItemInHand(i);
                                    player.getInventory().sendHeldItem(player.getViewers());
                                }
                            }
                            return true;
                        }

                        player.getInventory().sendContents(player);
                        target = player.getLevel().getBlock(blockVector);
                        BlockEntity blockEntity = player.getLevel().getLoadedBlockEntity(blockVector);

                        player.getLevel().sendBlocks(new Player[]{player}, new Block[]{target}, UpdateBlockPacket.FLAG_ALL_PRIORITY);

                        player.getInventory().sendHeldItem(player);

                        if (blockEntity != null && blockEntity.isSpawnable()) {
                            blockEntity.spawnTo(player);
                        }

                        return true;
                    case InventoryTransactionUtils.USE_ITEM_ACTION_CLICK_AIR:
                        Vector3f directionVector = player.getDirectionVector();

                        Item clientHand = Item.fromNetwork(packet.getItemInHand());
                        Item serverItem;

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

                        if (serverItem.onClickAir(player, directionVector)) {
                            if (player.isSurvival()) {
                                player.getInventory().setItemInHand(serverItem);
                            }

                            if (!player.isUsingItem()) {
                                player.setUsingItem(true);
                                return true;
                            }

                            // Used item
                            int ticksUsed = player.getServer().getTick() - player.getStartActionTick();
                            player.setUsingItem(false);

                            if (!serverItem.onUse(player, ticksUsed)) {
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

                Item clientHand = Item.fromNetwork(packet.getItemInHand());

                if (!clientHand.equalsExact(player.getInventory().getItemInHand())) {
                    player.getInventory().sendHeldItem(player);
                }

                Item serverItem = player.getInventory().getItemInHand();

                switch (packet.getActionType()) {
                    case InventoryTransactionUtils.USE_ITEM_ON_ENTITY_ACTION_INTERACT:
                        PlayerInteractEntityEvent playerInteractEntityEvent = new PlayerInteractEntityEvent(player, target, serverItem, packet.getClickPosition());
                        if (player.isSpectator()) playerInteractEntityEvent.setCancelled();
                        player.getServer().getEventManager().fire(playerInteractEntityEvent);

                        if (playerInteractEntityEvent.isCancelled()) {
                            break;
                        }
                        if (target.onInteract(player, serverItem, packet.getClickPosition()) && player.isSurvival()) {
                            if (serverItem.isTool()) {
                                if (serverItem.useOn(target) && serverItem.getMeta() >= serverItem.getMaxDurability()) {
                                    serverItem = Item.get(AIR, 0, 0);
                                }
                            } else {
                                if (serverItem.getCount() > 1) {
                                    serverItem.decrementCount();
                                } else {
                                    serverItem = Item.get(AIR, 0, 0);
                                }
                            }

                            player.getInventory().setItemInHand(serverItem);
                        }
                        break;
                    case InventoryTransactionUtils.USE_ITEM_ON_ENTITY_ACTION_ATTACK:
                        float itemDamage = serverItem.getAttackDamage();

                        for (Enchantment enchantment : serverItem.getEnchantments()) {
                            itemDamage += enchantment.getDamageBonus(target);
                        }

                        Map<EntityDamageEvent.DamageModifier, Float> damage = new EnumMap<>(EntityDamageEvent.DamageModifier.class);
                        damage.put(EntityDamageEvent.DamageModifier.BASE, itemDamage);

                        if (!player.canInteract(target.getPosition(), player.isCreative() ? 8 : 5)) {
                            break;
                        } else if (target instanceof Player) {
                            if (((Player) target).getGamemode() != GameMode.SURVIVAL) {
                                break;
                            } else if (!player.getServer().getConfig().isPVP()) {
                                break;
                            }
                        }

                        int knockback = 0;

                        Enchantment enchKnockback = player.getInventory().getItemInHand().getEnchantment(Enchantment.ID_KNOCKBACK);
                        if (enchKnockback != null) {
                            knockback = enchKnockback.getLevel();
                        }

                        EntityDamageByEntityEvent entityDamageByEntityEvent = new EntityDamageByEntityEvent(player, target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage);
                        if (player.isSpectator()) entityDamageByEntityEvent.setCancelled();
                        if ((target instanceof Player) && !player.getLevel().getGameRules().get(GameRules.PVP)) {
                            entityDamageByEntityEvent.setCancelled();
                        }

                        if (!target.attack(entityDamageByEntityEvent)) {
                            if (serverItem.isTool() && player.isSurvival()) {
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

                            for (Enchantment enchantment : serverItem.getEnchantments()) {
                                enchantment.doPostAttack(player, target);
                            }
                        }

                        if (serverItem.isTool() && player.isSurvival()) {
                            if (serverItem.useOn(target) && serverItem.getMeta() >= serverItem.getMaxDurability()) {
                                player.getInventory().setItemInHand(Item.get(AIR, 0, 0));
                            } else {
                                player.getInventory().setItemInHand(serverItem);
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
                                if (!serverItem.onRelease(player, ticksUsed)) {
                                    player.getInventory().sendContents(player);
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
            if (state.getType() == BlockIds.LECTERN) {
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
                    if (state.getType() == BlockIds.LECTERN) {
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
