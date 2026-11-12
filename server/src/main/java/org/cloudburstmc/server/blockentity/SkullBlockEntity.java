package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Skull;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.util.RedstoneSignals;

import java.util.Objects;

public class SkullBlockEntity extends BaseBlockEntity implements Skull {

    private CardinalDirection rotation = CardinalDirection.SOUTH;
    private boolean rotationDirty;
    private boolean powered;

    public SkullBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public CardinalDirection getRotation() {
        Direction facing = this.getBlockState().ensureTrait(BlockTraits.FACING_DIRECTION);
        return isVertical(facing) ? this.rotation : facing.getCardinalDirection();
    }

    @Override
    public void setRotation(CardinalDirection rotation) {
        Objects.requireNonNull(rotation, "rotation");
        if (!isVertical(this.getBlockState().ensureTrait(BlockTraits.FACING_DIRECTION))) {
            throw new IllegalStateException("Cannot rotate a wall-mounted head");
        }

        if (this.rotation != rotation) {
            this.rotation = rotation;
            this.rotationDirty = true;
            this.setDirty();
            this.scheduleUpdate();
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public boolean onUpdate() {
        boolean changed = this.updatePower();
        super.onUpdate();
        if (changed || this.rotationDirty) {
            this.rotationDirty = false;
            this.spawnToAll();
        }
        return false;
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        tag.listenForFloat("Rotation", yaw -> this.rotation = rotationFromYaw(yaw));
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        if (isVertical(this.getBlockState().ensureTrait(BlockTraits.FACING_DIRECTION))) {
            tag.putFloat("Rotation", this.rotation.ordinal() * 22.5f);
        } else {
            tag.remove("Rotation");
        }

        tag.putBoolean("MouthMoving", this.powered);
        tag.putBoolean("DoingAnimation", this.powered);
    }

    public static CardinalDirection rotationFromYaw(float yaw) {
        int segment = Math.floorMod(Math.round((yaw % 360f) / 22.5f), 16);
        return CardinalDirection.values()[segment];
    }

    private boolean updatePower() {
        BlockType type = this.getBlockState().getType();
        boolean powered = (type == BlockTypes.DRAGON_HEAD || type == BlockTypes.PIGLIN_HEAD) && RedstoneSignals.hasNeighborSignal(this.getBlock());
        boolean changed = this.powered != powered;
        this.powered = powered;
        return changed;
    }

    private static boolean isVertical(Direction facing) {
        return facing == Direction.UP || facing == Direction.DOWN;
    }
}
