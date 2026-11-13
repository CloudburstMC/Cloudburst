package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.FlowerPot;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.blockentity.data.BlockEntityData;
import org.cloudburstmc.server.blockentity.data.FlowerPotData;

import java.util.Objects;

public class FlowerPotBlockEntity extends BaseBlockEntity implements FlowerPot {

    private static final String PLANT_BLOCK = "PlantBlock";

    public FlowerPotBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return FlowerPotData.class;
    }

    @Override
    protected FlowerPotData data() {
        return (FlowerPotData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("item") || tag.containsKey("mData")) {
            throw new UnsupportedOperationException("Numeric flower pot data is no longer supported");
        }
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        super.saveCustomClient(tag);

        if (this.getPlant() == BlockStates.AIR) {
            tag.remove(PLANT_BLOCK);
        } else {
            tag.putCompound(PLANT_BLOCK, BlockPalette.INSTANCE.getSerialized(this.getPlant()));
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public BlockState getPlant() {
        return Objects.requireNonNullElse(this.data().getPlant(), BlockStates.AIR);
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

        if (this.getPlant() == blockState) {
            return;
        }

        this.data().setPlant(blockState == BlockStates.AIR ? null : blockState);
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
