package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.ShulkerBox;
import org.cloudburstmc.api.blockentity.ShulkerBoxAnimationState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.PushReaction;
import org.cloudburstmc.api.inventory.view.BlockStorageView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.MovementType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.packet.BlockEventPacket;
import org.cloudburstmc.server.block.util.ShulkerBoxGeometry;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Block entity implementation for a shulker box: a 27-slot storage container with an associated
 * facing direction that only accepts valid shulker box block types.
 */
public class ShulkerBoxBlockEntity extends ContainerBlockEntity implements ShulkerBox, BlockStorageView {
    private static final float ANIMATION_STEP = 0.1f;
    private static final float COLLISION_EPSILON = 1.0E-6f;

    private Direction facing = Direction.UP;
    private ShulkerBoxAnimationState animationState = ShulkerBoxAnimationState.CLOSED;
    private float openProgress;
    private float previousOpenProgress;
    private int viewerCount;

    public ShulkerBoxBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, new CloudContainer(27));
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.SHULKER_BOX;
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForList("Items", NbtType.COMPOUND, tags -> {
            for (NbtMap itemTag : tags) {
                ItemStack item = ItemUtils.deserializeItem(itemTag);
                this.container.setItem(itemTag.getByte("Slot"), item);
            }
        });
        this.facing = Direction.fromIndex(tag.getByte("facing"));
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        List<NbtMap> items = new ArrayList<>();
        this.container.forEachSlot((itemStack, slot) -> items.add(ItemUtils.serializeItem(itemStack, slot)));
        tag.putList("Items", NbtType.COMPOUND, items);
        this.saveFacing(tag);
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);
        this.saveFacing(tag);
    }

    private void saveFacing(NbtMapBuilder tag) {
        tag.putByte("facing", (byte) this.facing.getIndex());
    }

    @Override
    public Direction getFacing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        this.facing = Objects.requireNonNull(facing, "facing");
        this.setDirty();
    }

    @Override
    public ShulkerBoxAnimationState getAnimationState() {
        return this.animationState;
    }

    @Override
    public float getOpenProgress() {
        return this.openProgress;
    }

    @Override
    public float getPreviousOpenProgress() {
        return this.previousOpenProgress;
    }

    @Override
    public int getViewerCount() {
        return this.viewerCount;
    }

    public void startOpen() {
        this.viewerCount++;
        if (this.viewerCount == 1) {
            this.sendOpenState();
            this.animationState = ShulkerBoxAnimationState.OPENING;
            this.getLevel().addSound(this.getPosition(), Sound.RANDOM_SHULKERBOXOPEN, 0.5f, randomPitch());
            this.scheduleUpdate();
        }
    }

    public void stopOpen() {
        if (this.viewerCount == 0) {
            return;
        }

        this.viewerCount--;
        if (this.viewerCount == 0) {
            this.sendOpenState();
            this.animationState = ShulkerBoxAnimationState.CLOSING;
            this.getLevel().addSound(this.getPosition(), Sound.RANDOM_SHULKERBOXCLOSED, 0.5f, randomPitch());
            this.scheduleUpdate();
        }
    }

    public boolean canOpen() {
        BoundingBox openingSweep = lidMovementBox(this.getPosition(), this.facing, 0, 0.5f)
                .deflate(COLLISION_EPSILON, COLLISION_EPSILON, COLLISION_EPSILON);
        return this.animationState != ShulkerBoxAnimationState.CLOSED
                || !this.getLevel().hasCollision(null, openingSweep);
    }

    @Override
    public boolean onUpdate() {
        super.onUpdate();
        this.previousOpenProgress = this.openProgress;

        switch (this.animationState) {
            case CLOSED -> this.openProgress = 0;
            case OPENING -> {
                if (this.previousOpenProgress == 0) {
                    this.getLevel().updateAround(this.getPosition());
                }

                this.openProgress = Math.min(1, this.openProgress + ANIMATION_STEP);
                this.pushEntities();

                if (this.openProgress >= 1) {
                    this.animationState = ShulkerBoxAnimationState.OPEN;
                    this.getLevel().updateAround(this.getPosition());
                }
            }
            case OPEN -> this.openProgress = 1;
            case CLOSING -> {
                if (this.previousOpenProgress == 1) {
                    this.getLevel().updateAround(this.getPosition());
                }

                this.openProgress = Math.max(0, this.openProgress - ANIMATION_STEP);
                if (this.openProgress <= 0) {
                    this.animationState = ShulkerBoxAnimationState.CLOSED;
                    this.getLevel().updateAround(this.getPosition());
                }
            }
        }

        return this.animationState != ShulkerBoxAnimationState.CLOSED;
    }

    private void pushEntities() {
        BoundingBox box = ShulkerBoxGeometry.box(this.getPosition(), this.facing, this.openProgress);
        for (Entity entity : this.getLevel().getNearbyEntities(box)) {
            if (entity.getPushReaction() != PushReaction.IGNORE && entity instanceof CloudEntity cloudEntity) {
                BoundingBox entityBox = entity.getBoundingBox();
                float distance = switch (this.facing) {
                    case DOWN -> box.getMinY() - entityBox.getMaxY();
                    case UP -> box.getMaxY() - entityBox.getMinY();
                    case NORTH -> box.getMinZ() - entityBox.getMaxZ();
                    case SOUTH -> box.getMaxZ() - entityBox.getMinZ();
                    case WEST -> box.getMinX() - entityBox.getMaxX();
                    case EAST -> box.getMaxX() - entityBox.getMinX();
                };

                cloudEntity.move(MovementType.SHULKER_BOX,
                        distance * Math.abs(this.facing.getStepX()),
                        distance * Math.abs(this.facing.getStepY()),
                        distance * Math.abs(this.facing.getStepZ()));
            }
        }
    }

    private void sendOpenState() {
        BlockEventPacket packet = new BlockEventPacket();
        packet.setBlockPosition(this.getPosition());
        packet.setEventType(1);
        packet.setEventData(this.viewerCount > 0 ? 1 : 0);
        this.getLevel().addChunkPacket(this.getPosition(), packet);
    }

    private static float randomPitch() {
        return ThreadLocalRandom.current().nextFloat(0.9f, 1.0f);
    }

    static BoundingBox lidMovementBox(Vector3i position, Direction facing, float from, float to) {
        float min = Math.min(from, to);
        float max = Math.max(from, to);

        float x = position.getX();
        float y = position.getY();
        float z = position.getZ();

        return switch (facing) {
            case DOWN -> new BoundingBox(x, y - max, z, x + 1, y - min, z + 1);
            case UP -> new BoundingBox(x, y + 1 + min, z, x + 1, y + 1 + max, z + 1);
            case NORTH -> new BoundingBox(x, y, z - max, x + 1, y + 1, z - min);
            case SOUTH -> new BoundingBox(x, y, z + 1 + min, x + 1, y + 1, z + 1 + max);
            case WEST -> new BoundingBox(x - max, y, z, x - min, y + 1, z + 1);
            case EAST -> new BoundingBox(x + 1 + min, y, z, x + 1 + max, y + 1, z + 1);
        };
    }

    @Override
    public void close() {
        if (!closed) {
            for (ContainerListener listener : new HashSet<>(this.container.getListeners())) {
                if (listener instanceof CloudPlayer) {
                    ((CloudPlayer) listener).closeInventory();
                }
            }

            super.close();
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
