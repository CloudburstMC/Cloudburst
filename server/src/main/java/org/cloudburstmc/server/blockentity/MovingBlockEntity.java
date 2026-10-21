package org.cloudburstmc.server.blockentity;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.MovingBlock;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockStateCodec;
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
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        if (tag.containsKey("movingBlockId") || tag.containsKey("movingBlockData")) {
            throw new UnsupportedOperationException("Numeric moving block data is no longer supported");
        }

        this.blockState = BlockStateCodec.read(tag.getCompound("movingBlock"));
        this.extraBlockState = tag.containsKey("movingBlockExtra")
                ? BlockStateCodec.read(tag.getCompound("movingBlockExtra"))
                : BlockStates.AIR;

        tag.listenForCompound("movingEntity", entityTag -> {
            BlockEntityType<?> type = CloudBlockEntityRegistry.get().getBlockEntityType(entityTag.getString("id"));
            this.blockEntity = (BaseBlockEntity) CloudBlockEntityRegistry.get().newEntity(type, this.getChunk(), this.getPosition());
            this.blockEntity.loadAdditionalData(entityTag);
        });

        this.piston = Vector3i.from(tag.getInt("pistonPosX"), tag.getInt("pistonPosY"), tag.getInt("pistonPosZ"));
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putCompound("movingBlock", BlockStateCodec.write(this.blockState));
        tag.putCompound("movingBlockExtra", BlockStateCodec.write(this.extraBlockState));

        tag.putInt("pistonPosX", this.piston.getX());
        tag.putInt("pistonPosY", this.piston.getY());
        tag.putInt("pistonPosZ", this.piston.getZ());

        if (this.blockEntity != null) {
            tag.putCompound("movingEntity", this.blockEntity.getServerTag());
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
