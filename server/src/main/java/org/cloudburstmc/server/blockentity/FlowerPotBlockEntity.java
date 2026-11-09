package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.FlowerPot;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockStateCodec;

import java.util.Objects;

public class FlowerPotBlockEntity extends BaseBlockEntity implements FlowerPot {

    private static final String PLANT_BLOCK = "PlantBlock";

    private BlockState plant = BlockStates.AIR;

    public FlowerPotBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        if (tag.containsKey("item") || tag.containsKey("mData")) {
            throw new UnsupportedOperationException("Numeric flower pot data is no longer supported");
        }

        this.plant = tag.containsKey(PLANT_BLOCK, NbtType.COMPOUND) ? BlockStateCodec.read(tag.getCompound(PLANT_BLOCK)) : BlockStates.AIR;
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);

        if (this.plant == BlockStates.AIR) {
            tag.remove(PLANT_BLOCK);
        } else {
            tag.putCompound(PLANT_BLOCK, BlockPalette.INSTANCE.getSerialized(this.plant));
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public BlockState getPlant() {
        return this.plant;
    }

    @Override
    public void setPlant(BlockState blockState) {
        Objects.requireNonNull(blockState, "blockState");
        if (blockState != BlockStates.AIR && !blockState.is(BlockTags.POTTABLE_PLANTS)) {
            throw new IllegalArgumentException("Block cannot be potted: " + blockState);
        }

        if (this.isClosed() || !this.isValid()) {
            throw new IllegalStateException("Flower pot is no longer placed");
        }

        if (this.plant == blockState) {
            return;
        }

        this.plant = blockState;
        this.setDirty();

        Block block = this.getBlock();
        BlockState state = block.getState();
        BlockState updated = state.withTrait(BlockTraits.HAS_UPDATE, blockState != BlockStates.AIR);
        if (updated != state) {
            block.set(updated, true, false);
        }

        this.spawnToAll();
    }
}
