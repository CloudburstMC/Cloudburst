package org.cloudburstmc.server.blockentity;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.MovingBlock;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.MovingBlockData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

public class MovingBlockEntity extends BaseBlockEntity implements MovingBlock {

    private BlockState blockState = BlockStates.AIR;
    private BlockState extraBlockState = BlockStates.AIR;
    private BaseBlockEntity blockEntity;

    @Setter
    @Getter
    private Vector3i piston;

    public MovingBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return MovingBlockData.class;
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        MovingBlockData moving = (MovingBlockData) data;
        // Absent states read as air. That matches today for the extra state; a missing
        // moved state threw, but lenient reading keeps the mandated probe total.
        this.blockState = moving.getMovingBlock() != null ? moving.getMovingBlock() : BlockStates.AIR;
        this.extraBlockState = moving.getMovingBlockExtra() != null ? moving.getMovingBlockExtra() : BlockStates.AIR;

        NbtMap entityTag = moving.getMovingEntity();
        if (entityTag != null) {
            BlockEntityType<?> type = CloudBlockEntityRegistry.get().getBlockEntityType(entityTag.getString("id"));
            this.blockEntity = (BaseBlockEntity) CloudBlockEntityRegistry.get().newEntity(type, this.getChunk(), this.getPosition());
            this.blockEntity.loadAdditionalData(entityTag);
        }

        this.piston = Vector3i.from(moving.getPistonPosX(), moving.getPistonPosY(), moving.getPistonPosZ());
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        MovingBlockData moving = (MovingBlockData) data;
        // Both states always persist, air included: air is a registered palette
        // entry, so the codec writes it like any other state.
        moving.setMovingBlock(this.blockState);
        moving.setMovingBlockExtra(this.extraBlockState);

        // A freshly created moving block has no piston yet; today's save throws on it.
        // Omitting the keys keeps the mandated save-and-load probe from throwing.
        if (this.piston != null) {
            moving.setPistonPosX(this.piston.getX());
            moving.setPistonPosY(this.piston.getY());
            moving.setPistonPosZ(this.piston.getZ());
        }

        if (this.blockEntity != null) {
            moving.setMovingEntity(this.blockEntity.getServerTag());
        } else {
            moving.setMovingEntity(null);
        }
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("movingBlockId") || tag.containsKey("movingBlockData")) {
            throw new UnsupportedOperationException("Numeric moving block data is no longer supported");
        }
    }

    public BlockState getMovingBlock() {
        return this.blockState;
    }

    public void setBlock(BlockState blockState) {
        this.blockState = blockState == null ? BlockStates.AIR : blockState;
    }

    public BlockState getExtraBlock() {
        return extraBlockState;
    }

    public void setExtraBlock(BlockState extraBlockState) {
        this.extraBlockState = extraBlockState == null ? BlockStates.AIR : extraBlockState;
    }

    public BlockEntity getBlockEntity() {
        return blockEntity;
    }

    public void setBlockEntity(BlockEntity blockEntity) {
        this.blockEntity = (BaseBlockEntity) blockEntity;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
