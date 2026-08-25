package org.cloudburstmc.server.player.handler;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.google.inject.Inject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.ItemFrame;
import org.cloudburstmc.api.blockentity.Lectern;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.misc.ExperienceOrb;
import org.cloudburstmc.api.event.block.LecternPageChangeEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.player.*;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.FloatItemHandler;
import org.cloudburstmc.api.item.data.MapItem;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.chunk.LockableChunk;
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
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryActionData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventorySource;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.container.screen.CloudPlayerInventoryScreen;
import org.cloudburstmc.server.entity.projectile.EntityArrow;
import org.cloudburstmc.server.entity.vehicle.EntityAbstractMinecart;
import org.cloudburstmc.server.entity.vehicle.EntityBoat;
import org.cloudburstmc.server.event.server.DataPacketReceiveEvent;
import org.cloudburstmc.server.form.CustomForm;
import org.cloudburstmc.server.form.Form;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.item.ToolUtils;
import org.cloudburstmc.server.item.component.ArmorItemHandlers;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.particle.PunchBlockParticle;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.player.RespawnConfig;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.cloudburstmc.server.player.CloudPlayer.DEFAULT_SPEED;

/**
 * Handles all incoming packets from a connected player.
 */
@Log4j2
public class PlayerPacketHandler implements BedrockPacketHandler {
    private final CloudPlayer player;

    protected Vector3i lastRightClickPos = Vector3i.ZERO;
    protected double lastRightClickTime = 0.0;
    protected Direction lastRightClickFace = null;
    private boolean usingItemOnBlock;
    private int blockItemActivationTick = Integer.MIN_VALUE;

    @Inject
    GlobalRegistry globalRegistry;
    private Vector3i lastBreakPosition = Vector3i.ZERO;
    private @Nullable BlockBreakSession blockBreakSession;

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
    public PacketSignal handle(MovePlayerPacket packet) {
        log.debug("Received unexpected MovePlayerPacket from {}", player.getName());
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(PlayerAuthInputPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return PacketSignal.HANDLED;
        }

        player.setClientTick(packet.getTick());

        Set<PlayerAuthInputData> inputData = packet.getInputData();

        processInputFlags(inputData);
        processContinuousInputState(inputData);
        processVehicleInput(inputData);

        processMovement(packet);

        if (inputData.contains(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS)) {
            processBlockActions(packet);
        } else {
            refreshBlockBreak();
        }

        if (inputData.contains(PlayerAuthInputData.PERFORM_ITEM_STACK_REQUEST) && packet.getItemStackRequest() != null) {
            player.getItemStackNetManager().handleSingleRequest(packet.getItemStackRequest());
        }

        return PacketSignal.HANDLED;
    }

    private void processContinuousInputState(Set<PlayerAuthInputData> inputData) {
        boolean packetSneaking = inputData.contains(PlayerAuthInputData.SNEAKING);
        if (packetSneaking != player.isSneaking()) {
            player.setSneaking(packetSneaking);
        }
    }

    private void processMovement(PlayerAuthInputPacket packet) {
        if (player.getVehicle() != null) {
            processVehicleInput(packet);
            return;
        }

        if (player.getTeleportPosition() != null) {
            if (packet.getInputData().contains(PlayerAuthInputData.HANDLE_TELEPORT)) {
                Vector3f clientPosition = packet.getPosition().sub(0, player.getBaseOffset(), 0);
                if (!player.acknowledgeTeleport(clientPosition)) {
                    return;
                }
            } else {
                return;
            }
        }

        Vector3f rawPos = packet.getPosition();
        Vector3f rawRot = packet.getRotation();

        if (!Float.isFinite(rawPos.getX()) || !Float.isFinite(rawPos.getY()) || !Float.isFinite(rawPos.getZ()) || !Float.isFinite(rawRot.getX()) || !Float.isFinite(rawRot.getY()) || !Float.isFinite(rawRot.getZ())) {
            log.debug("[{}] movement packet dropped: non-finite position/rotation {}/{}", player.getName(), rawPos, rawRot);
            return;
        }

        boolean verticalCollision = packet.getInputData().contains(PlayerAuthInputData.VERTICAL_COLLISION);
        boolean horizontalCollision = packet.getInputData().contains(PlayerAuthInputData.HORIZONTAL_COLLISION);
        boolean onGround = verticalCollision && packet.getDelta().getY() <= 0;

        player.isCollidedVertically = verticalCollision;
        player.isCollidedHorizontally = horizontalCollision;
        player.setOnGround(onGround);

        Vector3f newPos = rawPos.sub(0, player.getBaseOffset(), 0);
        Vector3f currentPos = player.getPosition();

        float yaw = rawRot.getY() % 360;
        float pitch = rawRot.getX() % 360;
        if (yaw < 0) {
            yaw += 360;
        }

        final float ROT_EPSILON = 0.001f;
        boolean posUnchanged = newPos.distanceSquared(currentPos) < 0.01f;
        boolean rotUnchanged = Math.abs(yaw - player.getYaw()) < ROT_EPSILON && Math.abs(pitch - player.getPitch()) < ROT_EPSILON;
        if (posUnchanged && rotUnchanged) {
            return;
        }

        float maxDelta = player.getServer().getConfig().getMovement().getMaxPositionDelta();
        float distance = currentPos.distance(newPos);
        if (distance > maxDelta) {
            log.debug("[{}] movement corrected: claimed {} is {} blocks from current {}, exceeds max {}", player.getName(), newPos, String.format("%.2f", distance), currentPos, maxDelta);
            player.sendMovementCorrection(currentPos, player.getClientTick());
            return;
        }

        if (!player.isAlive() || !player.spawned) {
            log.debug("[{}] movement packet dropped: player not alive or not spawned (alive={} spawned={})", player.getName(), player.isAlive(), player.spawned);
            player.sendMovementCorrection(currentPos, player.getClientTick());
            return;
        }

        player.setRotation(yaw, pitch);
        player.setNewPosition(newPos);
        player.setForceMovement(null);

        if (player.getVehicle() instanceof EntityBoat) {
            player.getVehicle().setPositionAndRotation(newPos.sub(0, 1, 0), (yaw + 90) % 360, 0);
        }
    }

    private void processVehicleInput(PlayerAuthInputPacket packet) {
        player.setRotation(packet.getRotation().getY() % 360, packet.getRotation().getX() % 360);
        if (player.getVehicle() instanceof EntityAbstractMinecart minecart) {
            minecart.setInputMotionY(readForwardInput(packet));
        }
    }

    private float readForwardInput(PlayerAuthInputPacket packet) {
        InputMode inputMode = packet.getInputMode();
        boolean isMobileClassic = inputMode == InputMode.TOUCH && packet.getInputInteractionModel() == InputInteractionModel.CLASSIC;
        if (inputMode == InputMode.MOUSE || isMobileClassic) {
            Set<PlayerAuthInputData> inputData = packet.getInputData();
            if (inputData.contains(PlayerAuthInputData.UP)) {
                return 1.0f;
            } else if (inputData.contains(PlayerAuthInputData.DOWN)) {
                return -1.0f;
            }
            return 0.0f;
        }
        return packet.getAnalogMoveVector().getY();
    }

    private void processBlockActions(PlayerAuthInputPacket packet) {
        for (PlayerBlockActionData actionData : packet.getPlayerActions()) {
            Vector3i blockPos = actionData.getBlockPosition();

            switch (actionData.getAction()) {
                case START_BREAK:
                    handleStartBreak(blockPos, Direction.fromIndex(actionData.getFace()));
                    break;
                case ABORT_BREAK:
                case STOP_BREAK:
                    handleStopBreak(blockPos);
                    break;
                case CONTINUE_BREAK:
                case BLOCK_CONTINUE_DESTROY:
                    handleContinueBreak(blockPos, Direction.fromIndex(actionData.getFace()));
                    break;
                case BLOCK_PREDICT_DESTROY:
                    handleBlockPredictDestroy(blockPos, Direction.fromIndex(actionData.getFace()));
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
            int breakTicks = ToolUtils.getBreakTicks(player, player.getInventory().getSelectedItem(), targetState);
            if (breakTicks > 0) {
                sendBlockBreakEvent(LevelEvent.BLOCK_START_BREAK, blockPos, encodeBreakEventData(targetState));
                sendBlockBreakParticle(blockPos, targetState, face);
                this.blockBreakSession = new BlockBreakSession(blockPos, targetState, face, getDestroyProgress(targetState));
            } else {
                this.blockBreakSession = null;
            }
        } else {
            this.blockBreakSession = null;
        }

        player.breakingBlock = target;
        player.lastBreak = currentBreak;
        lastBreakPosition = blockPos;
    }

    private void handleStopBreak(Vector3i blockPos) {
        sendBlockBreakEvent(LevelEvent.BLOCK_STOP_BREAK, blockPos, 0);
        player.breakingBlock = null;
        this.blockBreakSession = null;
    }

    private void handleContinueBreak(Vector3i blockPos, Direction face) {
        if (!player.isBreakingBlock() || this.blockBreakSession == null) {
            startBreakFromContinue(blockPos, face);
            return;
        }

        if (!this.blockBreakSession.matches(blockPos)) {
            handleStopBreak(this.blockBreakSession.position());
            startBreakFromContinue(blockPos, face);
            return;
        }

        Block block = player.getLevel().getBlock(blockPos);
        BlockState state = block.getState();
        if (!state.equals(this.blockBreakSession.state())) {
            handleStopBreak(blockPos);
            return;
        }

        sendBlockBreakParticle(blockPos, state, face);

        int breakTicks = ToolUtils.getBreakTicks(player, player.getInventory().getSelectedItem(), state);
        if (breakTicks > 0) {
            this.blockBreakSession = this.blockBreakSession.withProgress(state, face, getDestroyProgress(state));
            sendBlockBreakEvent(LevelEvent.BLOCK_UPDATE_BREAK, blockPos, encodeBreakEventData(state));
        }
    }

    private void startBreakFromContinue(Vector3i blockPos, Direction face) {
        handleStartBreak(blockPos, face);

        if (this.blockBreakSession == null || !this.blockBreakSession.matches(blockPos)) {
            return;
        }

        BlockState state = this.blockBreakSession.state();
        int breakTicks = ToolUtils.getBreakTicks(player, player.getInventory().getSelectedItem(), state);
        if (breakTicks <= 0) {
            return;
        }

        sendBlockBreakEvent(LevelEvent.BLOCK_UPDATE_BREAK, blockPos, encodeBreakEventData(state));
    }

    private void refreshBlockBreak() {
        if (!player.isBreakingBlock() || this.blockBreakSession == null) {
            return;
        }

        Vector3i blockPos = this.blockBreakSession.position();
        BlockState state = player.getLevel().getBlock(blockPos).getState();
        if (!state.equals(this.blockBreakSession.state())) {
            handleStopBreak(blockPos);
            return;
        }

        int breakTicks = ToolUtils.getBreakTicks(player, player.getInventory().getSelectedItem(), state);
        if (breakTicks > 0) {
            this.blockBreakSession = this.blockBreakSession.withProgress(state, this.blockBreakSession.face(), getDestroyProgress(state));
            if (player.getServer().getTick() % 5 == 0) {
                sendBlockBreakParticle(blockPos, state, this.blockBreakSession.face());
            }
            sendBlockBreakEvent(LevelEvent.BLOCK_UPDATE_BREAK, blockPos, encodeBreakEventData(state));
        }
    }

    private void handleBlockPredictDestroy(Vector3i blockPos, Direction face) {
        if (!player.spawned || !player.isAlive()) {
            return;
        }

        Block predictedBlock = player.getLevel().getBlock(blockPos);
        BlockState predictedState = predictedBlock.getState();
        boolean hasMatchingSession = this.blockBreakSession != null
                && this.blockBreakSession.matches(blockPos)
                && this.blockBreakSession.state().equals(predictedState);
        if (!hasMatchingSession) {
            startBreakFromContinue(blockPos, face);
        }
        Boolean fastBreak = hasMatchingSession ? getPredictedFastBreak(blockPos, predictedState) : null;

        player.breakingBlock = null;
        this.blockBreakSession = null;

        sendBlockBreakEvent(LevelEvent.BLOCK_STOP_BREAK, blockPos, 0);
        sendBlockBreakEvent(LevelEvent.BLOCK_START_BREAK, blockPos, encodeBreakEventData(predictedState));

        ItemStack selectedItem = player.getInventory().getSelectedItem();
        ItemStack oldItem = selectedItem;

        if (player.canInteract(blockPos.toFloat().add(0.5f, 0.5f, 0.5f), player.isCreative() ? 13 : 7)) {
            selectedItem = player.getLevel().breakBlockPredicted(blockPos, selectedItem, player, true, fastBreak);
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

    private void sendBlockBreakEvent(LevelEvent event, Vector3i blockPos, int data) {
        LevelEventPacket levelEvent = new LevelEventPacket();
        levelEvent.setType(event);
        levelEvent.setPosition(blockPos.toFloat());
        levelEvent.setData(data);
        player.sendPacket(levelEvent);
        player.getLevel().addChunkPacket(blockPos, levelEvent);
    }

    private void sendBlockBreakParticle(Vector3i blockPos, BlockState state, Direction face) {
        if (state.equals(BlockStates.AIR)) {
            return;
        }

        Vector3f position = blockPos.toFloat().add(0.5f, 0.5f, 0.5f);
        for (BedrockPacket packet : new PunchBlockParticle(position, state, face).encode()) {
            player.sendPacket(packet);
            player.getLevel().addChunkPacket(blockPos, packet);
        }
    }

    private @Nullable Boolean getPredictedFastBreak(Vector3i blockPos, BlockState state) {
        if (this.blockBreakSession == null || !this.blockBreakSession.matches(blockPos) || !this.blockBreakSession.state().equals(state)) {
            return null;
        }

        return this.blockBreakSession.progress() < 0.7D;
    }

    private int encodeBreakEventData(BlockState state) {
        double progress = getDestroyProgress(state);
        if (progress <= 0 || !Double.isFinite(progress)) {
            return 0;
        }
        return Math.max(1, (int) (65535 * progress));
    }

    private double getDestroyProgress(BlockState state) {
        return ToolUtils.getDestroyProgress(player, player.getInventory().getSelectedItem(), state);
    }

    private record BlockBreakSession(Vector3i position, BlockState state, Direction face, double progress) {

        private boolean matches(Vector3i position) {
            return this.position.equals(position);
        }

        private BlockBreakSession withProgress(BlockState state, Direction face, double progress) {
            return new BlockBreakSession(this.position, state, face, this.progress + progress);
        }
    }

    /**
     * Returns {@code true} if the item the player's game claims to be holding does not match what the server has
     * in the selected hotbar slot. When this occurs the interaction must be rejected and the player's
     * inventory resynchronized to avoid ghost items.
     */
    private boolean isHeldItemDesynced(@Nullable ItemUseTransaction transaction) {
        if (transaction == null) {
            return false;
        }
        return isHeldItemDesynced(transaction.getItemInHand());
    }

    private boolean isHeldItemDesynced(@Nullable ItemData clientItemData) {
        ItemStack clientItem = clientItemData == null
                ? ItemStack.EMPTY
                : ItemUtils.fromNetwork(clientItemData);
        ItemStack serverItem = player.getInventory().getSelectedItem();
        return !serverItem.isSimilar(clientItem);
    }

    private void handleItemUseOnBlock(ItemUseTransaction transaction, Vector3i blockPos, Direction face, Vector3f clickPos) {
        boolean spamBug = System.currentTimeMillis() - lastRightClickTime < 110.0
                && blockPos.distanceSquared(lastRightClickPos) < 0.00001
                && face == lastRightClickFace;
        lastRightClickPos = blockPos;
        lastRightClickFace = face;
        if (spamBug) {
            player.sendHeldItemSlot();
            return;
        }
        lastRightClickTime = System.currentTimeMillis();

        if (isHeldItemDesynced(transaction)) {
            rollbackBlock(blockPos, face);
            return;
        }

        if (!player.canInteract(blockPos.toFloat().add(0.5f, 0.5f, 0.5f), player.isCreative() ? 13 : 7)) {
            rollbackBlock(blockPos, face);
            return;
        }

        CloudLevel level = player.getLevel();
        Block target = level.getBlock(blockPos);
        Block side = target.getSide(face);
        ItemStack item = player.getInventory().getSelectedItem();

        if (level.tryUseBlock(target, side, face, item, player)) {
            return;
        }

        if (!item.isEmpty()) {
            ItemStack afterUse = level.tryUseItem(target, face, clickPos, item, player);
            if (afterUse != null) {
                player.getInventory().setSelectedItem(afterUse);
                rollbackBlock(blockPos, face);
                return;
            }
        }

        if (!item.isEmpty()) {
            ItemStack afterPlace = level.tryPlaceBlock(target, side, face, clickPos, item, player, true);
            if (afterPlace != null) {
                player.getInventory().setSelectedItem(afterPlace);
                return;
            }
        }

        if (!item.isEmpty()) {
            ItemStack afterUse = level.tryActivateItem(item, player);
            if (afterUse != null) {
                this.blockItemActivationTick = player.getServer().getTick();
                player.getInventory().setSelectedItem(afterUse);
                rollbackBlock(blockPos, face);
                return;
            }
        }

        rollbackBlock(blockPos, face);
    }

    private void handleItemUseInAir(@Nullable ItemUseTransaction transaction, Direction face) {
        ItemData clientItemData = transaction != null ? transaction.getItemInHand() : null;
        handleItemUseInAir(clientItemData, face);
    }

    private void handleItemUseInAir(@Nullable ItemData clientItemData, Direction face) {
        if (this.usingItemOnBlock || this.blockItemActivationTick == player.getServer().getTick()) {
            return;
        }

        ItemStack useItem = player.getInventory().getSelectedItem();
        if (isHeldItemDesynced(clientItemData)) {
            player.sendHeldItemSlot();
            return;
        }

        if (player.isUsingItem() && ArmorItemHandlers.armorSlot(useItem) == -1) {
            return;
        }

        Vector3f directionVector = player.getDirectionVector();
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, useItem, directionVector, face, PlayerInteractEvent.Action.RIGHT_CLICK_AIR);
        player.getServer().getEventManager().fire(interactEvent);

        if (interactEvent.isCancelled()) {
            player.sendHeldItemSlot();
            return;
        }

        if (!useItem.isEmpty()) {
            CloudLevel level = player.getLevel();
            ItemStack afterUse = level.tryActivateItem(useItem, player);
            if (afterUse != null) {
                player.getInventory().setSelectedItem(afterUse);
            }
        }
    }

    private void rollbackBlock(Vector3i blockPos, Direction face) {
        if (blockPos.distanceSquared(player.getPosition().toInt()) > 10000) {
            return;
        }
        Block target = player.getLevel().getBlock(blockPos);
        Block adjacent = target.getSide(face);
        player.getLevel().sendBlocks(new CloudPlayer[]{player}, new Block[]{target, adjacent}, UpdateBlockPacket.FLAG_ALL_PRIORITY);
        player.sendHeldItemSlot();
    }

    private void processInputFlags(Set<PlayerAuthInputData> inputData) {
        processGlidingInput(inputData);

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
                case STOP_GLIDING:
                    break;
                case START_CRAWLING:
                    PlayerToggleCrawlEvent startCrawlEvent = new PlayerToggleCrawlEvent(player, true);
                    player.getServer().getEventManager().fire(startCrawlEvent);
                    if (startCrawlEvent.isCancelled()) {
                        player.sendFlags(player);
                    } else {
                        player.setCrawling(true);
                    }
                    break;
                case STOP_CRAWLING:
                    PlayerToggleCrawlEvent stopCrawlEvent = new PlayerToggleCrawlEvent(player, false);
                    player.getServer().getEventManager().fire(stopCrawlEvent);
                    if (stopCrawlEvent.isCancelled()) {
                        player.sendFlags(player);
                    } else {
                        player.setCrawling(false);
                    }
                    break;
                case START_SPIN_ATTACK:
                    break;
                case STOP_SPIN_ATTACK:
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
                    if (!player.getServer().getAllowFlight() && !player.getAbilities().get(org.cloudburstmc.api.player.Ability.MAY_FLY)) {
                        player.kick(PlayerKickEvent.Reason.FLYING_DISABLED, "Flying is not enabled on player server");
                    } else {
                        PlayerToggleFlightEvent flightEvent = new PlayerToggleFlightEvent(player, true);
                        player.getServer().getEventManager().fire(flightEvent);
                        if (flightEvent.isCancelled()) {
                            player.getAbilities().update();
                        } else {
                            player.getAbilities().set(org.cloudburstmc.api.player.Ability.FLYING, true);
                            player.getAbilities().update();
                        }
                    }
                    break;
                case STOP_FLYING:
                    PlayerToggleFlightEvent flightEvent = new PlayerToggleFlightEvent(player, false);
                    player.getServer().getEventManager().fire(flightEvent);
                    if (flightEvent.isCancelled()) {
                        player.getAbilities().update();
                    } else {
                        player.getAbilities().set(org.cloudburstmc.api.player.Ability.FLYING, false);
                        player.getAbilities().update();
                    }
                    break;
                default:
                    break;
            }
        }
        player.getData().update();
    }

    private void processVehicleInput(Set<PlayerAuthInputData> inputData) {
        if (player.getVehicle() instanceof EntityBoat boat && boat.isControlling(player)) {
            boat.setPaddling(
                    inputData.contains(PlayerAuthInputData.PADDLE_LEFT),
                    inputData.contains(PlayerAuthInputData.PADDLE_RIGHT)
            );
        }
    }

    private void processGlidingInput(Set<PlayerAuthInputData> inputData) {
        Set<PlayerAuthInputData> remainingInput = new HashSet<>(inputData);
        for (PlayerAuthInputData input : inputData) {
            remainingInput.remove(input);
            switch (input) {
                case START_GLIDING:
                    if (!remainingInput.contains(PlayerAuthInputData.STOP_GLIDING)) {
                        player.tryStartGliding();
                    }
                    break;
                case STOP_GLIDING:
                    if (player.isGliding()) {
                        player.stopGliding();
                    } else {
                        player.sendFlags(player);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public PacketSignal handle(RequestAbilityPacket packet) {
        if (packet.getType() != org.cloudburstmc.protocol.bedrock.data.Ability.Type.BOOLEAN) {
            return PacketSignal.HANDLED;
        }

        org.cloudburstmc.protocol.bedrock.data.Ability requested = packet.getAbility();
        if (requested == org.cloudburstmc.protocol.bedrock.data.Ability.FLYING) {
            boolean wantsToFly = packet.isBoolValue();
            if (wantsToFly && !player.getAbilities().get(org.cloudburstmc.api.player.Ability.MAY_FLY)) {
                player.getAbilities().update();
                return PacketSignal.HANDLED;
            }

            PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(player, wantsToFly);
            player.getServer().getEventManager().fire(event);
            if (event.isCancelled()) {
                player.getAbilities().update();
            } else {
                player.getAbilities().set(org.cloudburstmc.api.player.Ability.FLYING, event.isFlying());
                player.getAbilities().update();
            }
            return PacketSignal.HANDLED;
        }

        if (requested == org.cloudburstmc.protocol.bedrock.data.Ability.NO_CLIP) {
            if (!player.getAbilities().get(org.cloudburstmc.api.player.Ability.NO_CLIP)) {
                player.getAbilities().update();
            }
            return PacketSignal.HANDLED;
        }

        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(EmotePacket packet) {
        if (!player.isSpawned()) {
            return PacketSignal.HANDLED;
        }
        if (packet.getRuntimeEntityId() != player.getRuntimeId()) {
            log.warn("{} sent EmotePacket with invalid entity id: {} != {}", player.getName(), packet.getRuntimeEntityId(), player.getRuntimeId());
            return PacketSignal.HANDLED;
        }
        for (CloudPlayer p : this.player.getViewers()) {
            p.sendPacket(packet);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(PacketViolationWarningPacket packet) {
        log.warn("Received packet violation warning: {}", packet.toString());
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(MobEquipmentPacket packet) {
        if (!player.spawned || !player.isAlive()) {
            return PacketSignal.HANDLED;
        }

        if (packet.getContainerId() != ContainerId.INVENTORY) {
            return PacketSignal.HANDLED;
        }

        int newSlot = packet.getHotbarSlot();
        if (newSlot < 0 || newSlot > 8) {
            player.sendHeldItemSlot();
            return PacketSignal.HANDLED;
        }

        if (player.getSelectedHotbarSlot() != newSlot) {
            applyClientHotbarSlot(newSlot);
        }
        player.setUsingItem(false);

        return PacketSignal.HANDLED;
    }

    private void applyClientHotbarSlot(int slot) {
        ItemStack heldItem = player.getInventory().getItem(slot);
        PlayerItemHeldEvent event = new PlayerItemHeldEvent(player, heldItem, slot);
        player.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            player.sendHeldItemSlot();
            return;
        }
        player.acknowledgeHotbarSlot(slot);
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
            case START_ITEM_USE_ON:
                this.usingItemOnBlock = true;
                break;
            case STOP_ITEM_USE_ON:
                this.usingItemOnBlock = false;
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

                player.closeInventory();

                Location respawnLoc = player.findRespawnPosition();
                boolean isBedSpawn = false;
                boolean isAnchorSpawn = false;

                Set<PlayerRespawnEvent.RespawnFlag> respawnFlags = EnumSet.noneOf(PlayerRespawnEvent.RespawnFlag.class);
                if (respawnLoc != null) {
                    RespawnConfig cfg = player.getRespawnConfig();
                    if (cfg != null) {
                        if (cfg.spawnType() == RespawnConfig.SpawnType.BED) {
                            respawnFlags.add(PlayerRespawnEvent.RespawnFlag.BED_SPAWN);
                            isBedSpawn = true;
                        } else {
                            respawnFlags.add(PlayerRespawnEvent.RespawnFlag.ANCHOR_SPAWN);
                            isAnchorSpawn = true;
                        }
                    }
                } else {
                    respawnLoc = player.getServer().getDefaultLevel().getSafeSpawn();
                }

                PlayerRespawnEvent playerRespawnEvent = new PlayerRespawnEvent(player, respawnLoc, respawnFlags);
                player.getServer().getEventManager().fire(playerRespawnEvent);
                respawnLoc = playerRespawnEvent.getRespawnLocation();

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

                player.getAbilities().update();
                player.getInventoryManager().sendAllInventories();

                player.spawnToAll();
                player.scheduleUpdate();

                PlayerPostRespawnEvent postRespawnEvent = new PlayerPostRespawnEvent(player, respawnLoc, isBedSpawn, isAnchorSpawn);
                player.getServer().getEventManager().fire(postRespawnEvent);
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

            if (response.isNull()) {
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
            log.debug("Got block-pick request from {} when in spectator mode", player.getName());
            return PacketSignal.HANDLED;
        }

        Vector3i pickPos = packet.getBlockPosition();
        Block block = player.getLevel().getBlock(pickPos.getX(), pickPos.getY(), pickPos.getZ());

        if (block.getState().getType() == BlockTypes.AIR) {
            log.debug("Got block-pick request from {} for air block", player.getName());
            return PacketSignal.HANDLED;
        }

        ItemStack item = block.requireComponent(BlockComponents.GET_PICK_BLOCK)
                .execute(block);
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
                if (pickBlockMatchesDamage(player.getContainer().getItem(slot), item)) {
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
                        player.setSelectedHotbarSlot(slot);
                        player.getInventory().setSelectedItem(item);
                        return PacketSignal.HANDLED;
                    } else if (itemSlot > -1) {
                        player.setSelectedHotbarSlot(slot);
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

        PlayerAnimationEvent.Type animationType = switch (packet.getAction()) {
            case NO_ACTION -> PlayerAnimationEvent.Type.NO_ACTION;
            case SWING_ARM -> PlayerAnimationEvent.Type.SWING_ARM;
            case WAKE_UP -> PlayerAnimationEvent.Type.WAKE_UP;
            case CRITICAL_HIT -> PlayerAnimationEvent.Type.CRITICAL_HIT;
            case MAGIC_CRITICAL_HIT -> PlayerAnimationEvent.Type.MAGIC_CRITICAL_HIT;
            default -> null;
        };

        if (animationType == null) {
            return PacketSignal.HANDLED;
        }

        PlayerAnimationEvent animationEvent = new PlayerAnimationEvent(player, animationType);
        player.getServer().getEventManager().fire(animationEvent);
        if (animationEvent.isCancelled()) {
            return PacketSignal.HANDLED;
        }

        AnimatePacket animatePacket = new AnimatePacket();
        animatePacket.setRuntimeEntityId(player.getRuntimeId());
        animatePacket.setAction(switch (animationEvent.getAnimationType()) {
            case NO_ACTION -> AnimatePacket.Action.NO_ACTION;
            case SWING_ARM -> AnimatePacket.Action.SWING_ARM;
            case WAKE_UP -> AnimatePacket.Action.WAKE_UP;
            case CRITICAL_HIT -> AnimatePacket.Action.CRITICAL_HIT;
            case MAGIC_CRITICAL_HIT -> AnimatePacket.Action.MAGIC_CRITICAL_HIT;
        });
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
            player.getServer().dispatchCommand(playerCommandPreprocessEvent.getPlayer(), playerCommandPreprocessEvent.getMessage().substring(1));
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
    public PacketSignal handle(InventoryContentPacket packet) {
        // Clients must not be allowed to overwrite server inventory state.
        // Re-sync all inventories so the client reflects the authoritative state.
        if (player.spawned) {
            player.getInventoryManager().sendAllInventories();
        }
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
        player.setChunkRadius(packet.getRadius());
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(SetPlayerGameTypePacket packet) {
        SetPlayerGameTypePacket correction = new SetPlayerGameTypePacket();
        correction.setGamemode(player.getGameMode().getVanillaId());
        player.sendPacket(correction);
        player.getAbilities().update();
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

        int packetHotbarSlot = packet.getHotbarSlot();
        if (packetHotbarSlot >= 0 && packetHotbarSlot <= 8 && player.getSelectedHotbarSlot() != packetHotbarSlot) {
            applyClientHotbarSlot(packetHotbarSlot);
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
                        if (slot < 0 || slot > 8) {
                            return PacketSignal.HANDLED;
                        }

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
                    case 0:
                        handleItemUseOnBlock(
                                null,
                                packet.getBlockPosition(),
                                Direction.fromIndex(packet.getBlockFace()),
                                packet.getClickPosition()
                        );
                        break;
                    case 1:
                        handleItemUseInAir(packet.getItemInHand(), Direction.fromIndex(packet.getBlockFace()));
                        break;
                    case 2: // break-block no-op
                        break;
                    case 3:
                        if (player.getInventory().getSelectedItem().getType() == ItemTypes.TRIDENT) {
                            player.setUsingItem(false);
                            AnimatePacket animPkt = new AnimatePacket();
                            animPkt.setAction(AnimatePacket.Action.SWING_ARM);
                            animPkt.setRuntimeEntityId(player.getRuntimeId());
                            CloudServer.broadcastPacket(player.getViewers(), animPkt);
                        }
                        break;
                    default:
                        break;
                }
                player.getItemStackNetManager().acknowledgeLegacyTransaction(packet.getLegacyRequestId(), packet.getLegacySlots());
                return PacketSignal.HANDLED;
            case ITEM_USE_ON_ENTITY: {
                Entity target = player.getLevel().getEntity(packet.getRuntimeEntityId());
                if (target == null || !target.isAlive()) {
                    break;
                }

                if (target instanceof DroppedItem || target instanceof EntityArrow || target instanceof ExperienceOrb) {
                    break;
                }

                if (!player.canInteract(target.getPosition(), player.isCreative() ? 13 : 7)) {
                    break;
                }

                ItemStack heldItem = player.getInventory().getSelectedItem();
                Vector3f clickPos = packet.getClickPosition();
                switch (packet.getActionType()) {
                    case 0: { // interact with entity
                        PlayerInteractEntityEvent interactEntityEvent = new PlayerInteractEntityEvent(player, target, heldItem, clickPos);
                        player.getServer().getEventManager().fire(interactEntityEvent);
                        if (interactEntityEvent.isCancelled()) {
                            break;
                        }
                        target.onInteract(player, heldItem, clickPos);
                        break;
                    }
                    case 1: { // attack entity
                        PlayerInteractEntityEvent attackEntityEvent = new PlayerInteractEntityEvent(player, target, heldItem, clickPos);
                        player.getServer().getEventManager().fire(attackEntityEvent);
                        if (attackEntityEvent.isCancelled()) {
                            break;
                        }

                        float damage = 0f;
                        FloatItemHandler attackBonus = CloudItemRegistry.get().requireComponent(heldItem.getType(), ItemComponents.GET_ATTACK_DAMAGE_BONUS);
                        if (attackBonus != null) {
                            damage = attackBonus.execute(heldItem);
                        }

                        if (damage <= 0f) {
                            damage = 1f;
                        }

                        DamageSource source = DamageSource.builder(DamageTypes.ENTITY_ATTACK)
                                .directEntity(player).causingEntity(player).location(player.getLocation()).build();
                        EntityDamageEvent damageEvent = new EntityDamageEvent(target, source, damage);
                        target.attack(damageEvent);

                        AnimatePacket swingPkt = new AnimatePacket();
                        swingPkt.setAction(AnimatePacket.Action.SWING_ARM);
                        swingPkt.setRuntimeEntityId(player.getRuntimeId());
                        CloudServer.broadcastPacket(player.getViewers(), swingPkt);
                        break;
                    }
                    default:
                        break;
                }
                break;
            }
            case ITEM_RELEASE:
                if (packet.getActionType() == 0) {
                    // TODO: trigger full item-release completion handler (bow arrow spawn, etc.)
                    player.setUsingItem(false);
                }
                break;
            default:
                player.getInventoryManager().sendAllInventories();
                break;
        }
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
            Location respawnPos = player.findRespawnPosition();
            if (respawnPos == null) {
                respawnPos = player.getServer().getDefaultLevel().getSafeSpawn();
            }
            RespawnPacket respawn1 = new RespawnPacket();
            respawn1.setPosition(respawnPos.getPosition());
            respawn1.setState(RespawnPacket.State.SERVER_READY);
            player.sendPacket(respawn1);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LecternUpdatePacket packet) {
        Vector3i blockPosition = packet.getBlockPosition();

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
                    block.requireComponent(BlockComponents.ON_REDSTONE_UPDATE).execute(block);
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
                Component.translatable("multiplayer.player.joined", player.displayName()).color(NamedTextColor.YELLOW)
        );

        player.getServer().getEventManager().fire(playerJoinEvent);

        if (playerJoinEvent.getJoinMessage() != null) {
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

        Long2IntMap servedPerColumn = new Long2IntOpenHashMap();
        for (Vector3i offset : packet.getPositionOffsets()) {
            int sectionY = center.getY() + offset.getY();
            int chunkX = center.getX() + offset.getX();
            int chunkZ = center.getZ() + offset.getZ();

            SubChunkData subChunkData = new SubChunkData();
            subChunkData.setPosition(offset);

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
                            // Empty column, treat as below all sections
                            heightSectionCoord = minSectionY - 1;
                        } else {
                            heightSectionCoord = highestY >> 4;
                        }
                        int idx = (hz << 4) | hx;
                        if (heightSectionCoord > sectionY) {
                            heightMap[idx] = 16;
                            allLower = false;
                        } else if (heightSectionCoord < sectionY) {
                            heightMap[idx] = -1;
                            allHigher = false;
                        } else {
                            heightMap[idx] = (byte) (highestY & 0xf);
                            allHigher = false;
                            allLower = false;
                        }
                    }
                }

                HeightMapDataType hMapType;
                if (allHigher) {
                    hMapType = HeightMapDataType.TOO_HIGH;
                } else if (allLower) {
                    hMapType = HeightMapDataType.TOO_LOW;
                } else {
                    hMapType = HeightMapDataType.HAS_DATA;
                }
                subChunkData.setHeightMapType(hMapType);
                subChunkData.setRenderHeightMapType(hMapType);
                if (hMapType == HeightMapDataType.HAS_DATA) {
                    subChunkData.setHeightMapData(Unpooled.copiedBuffer(heightMap));
                    subChunkData.setRenderHeightMapData(Unpooled.copiedBuffer(heightMap));
                }

                if (section == null || section.isEmpty()) {
                    subChunkData.setResult(SubChunkRequestResult.SUCCESS_ALL_AIR);
                } else {
                    subChunkData.setResult(SubChunkRequestResult.SUCCESS);

                    ByteBuf sectionBuf = ByteBufAllocator.DEFAULT.ioBuffer();
                    try {
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
                        sectionBuf = null;
                    } finally {
                        if (sectionBuf != null) {
                            sectionBuf.release();
                        }
                    }
                }
            } finally {
                locked.unlock();
            }

            SubChunkRequestResult result = subChunkData.getResult();
            if (result == SubChunkRequestResult.SUCCESS || result == SubChunkRequestResult.SUCCESS_ALL_AIR) {
                long columnKey = CloudChunk.key(chunkX, chunkZ);
                servedPerColumn.mergeInt(columnKey, 1, Integer::sum);
            }

            responseChunks.add(subChunkData);
        }

        SubChunkPacket response = new SubChunkPacket();
        response.setDimension(packet.getDimension());
        response.setCenterPosition(center);
        response.setSubChunks(responseChunks);
        player.sendPacket(response);

        servedPerColumn.long2IntEntrySet().forEach(entry -> {
            long key = entry.getLongKey();
            int chunkX2 = CloudChunk.fromKeyX(key);
            int chunkZ2 = CloudChunk.fromKeyZ(key);
            player.recordSubChunkServed(chunkX2, chunkZ2, entry.getIntValue());
        });

        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ServerboundLoadingScreenPacket packet) {
        if (packet.getType() == ServerboundLoadingScreenPacketType.END_LOADING_SCREEN) {
            player.setChangingDimension(false);
        }
        return PacketSignal.HANDLED;
    }

    /**
     * Returns {@code true} if {@code inventoryItem} has the same type as
     * {@code pickedItem} and carries the same {@link ItemKeys#DAMAGE} value.
     */
    private static boolean pickBlockMatchesDamage(ItemStack inventoryItem, ItemStack pickedItem) {
        if (!inventoryItem.isSimilar(pickedItem)) {
            return false;
        }
        return inventoryItem.getDamage() == pickedItem.getDamage();
    }
}
