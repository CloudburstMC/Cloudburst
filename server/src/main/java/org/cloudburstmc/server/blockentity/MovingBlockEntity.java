package org.cloudburstmc.server.blockentity;

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
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

import java.util.Set;

public class MovingBlockEntity extends BaseBlockEntity implements MovingBlock {

    /**
     * The live nested block entity deserialized from the DTO's tag. It is a runtime
     * view, not a second copy of state: the tag in the DTO is re-serialized from it
     * on every save, like the previous fillData did.
     */
    private BaseBlockEntity blockEntity;

    public MovingBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return MovingBlockData.class;
    }

    @Override
    protected MovingBlockData data() {
        return (MovingBlockData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("movingBlockId") || tag.containsKey("movingBlockData")) {
            throw new UnsupportedOperationException("Numeric moving block data is no longer supported");
        }

        data().setPiston(Vector3i.from(tag.getInt("pistonPosX", 0), tag.getInt("pistonPosY", 0),
                tag.getInt("pistonPosZ", 0)));

        NbtMap entityTag = data().getMovingEntity();
        if (entityTag != null) {
            BlockEntityType<?> type = CloudBlockEntityRegistry.get().getBlockEntityType(entityTag.getString("id"));
            this.blockEntity = (BaseBlockEntity) CloudBlockEntityRegistry.get().newEntity(type, this.getChunk(), this.getPosition());
            this.blockEntity.loadAdditionalData(entityTag);
        } else {
            this.blockEntity = null;
        }
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        Vector3i piston = data().getPiston();
        if (piston != null) {
            tag.putInt("pistonPosX", piston.getX());
            tag.putInt("pistonPosY", piston.getY());
            tag.putInt("pistonPosZ", piston.getZ());
        }
        // A cleared nested entity is not written; a live one is re-serialized,
        // refreshing whatever changed since the load.
        if (this.blockEntity != null) {
            tag.putCompound("movingEntity", this.blockEntity.getServerTag());
        }
    }

    @Override
    protected Set<String> customKeys() {
        return Set.of("pistonPosX", "pistonPosY", "pistonPosZ");
    }

    public BlockState getMovingBlock() {
        BlockState movingBlock = data().getMovingBlock();
        return movingBlock != null ? movingBlock : BlockStates.AIR;
    }

    public void setBlock(BlockState blockState) {
        data().setMovingBlock(blockState == null ? BlockStates.AIR : blockState);
    }

    public BlockState getExtraBlock() {
        BlockState extraBlockState = data().getMovingBlockExtra();
        return extraBlockState != null ? extraBlockState : BlockStates.AIR;
    }

    public void setExtraBlock(BlockState extraBlockState) {
        data().setMovingBlockExtra(extraBlockState == null ? BlockStates.AIR : extraBlockState);
    }

    public BlockEntity getBlockEntity() {
        return blockEntity;
    }

    public void setBlockEntity(BlockEntity blockEntity) {
        this.blockEntity = (BaseBlockEntity) blockEntity;
        data().setMovingEntity(blockEntity == null ? null : ((BaseBlockEntity) blockEntity).getServerTag());
    }

    public Vector3i getPiston() {
        return data().getPiston();
    }

    public void setPiston(Vector3i piston) {
        data().setPiston(piston);
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
