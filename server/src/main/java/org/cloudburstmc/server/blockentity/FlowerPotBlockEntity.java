package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.FlowerPot;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockStateCodec;


public class FlowerPotBlockEntity extends BaseBlockEntity implements FlowerPot {

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

        if (tag.containsKey("PlantBlock", NbtType.COMPOUND)) {
            NbtMap plantTag = tag.getCompound("PlantBlock");

            this.plant = BlockStateCodec.read(plantTag);
        }
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        if (this.plant != BlockStates.AIR) {
            tag.putCompound("PlantBlock", BlockPalette.INSTANCE.getSerialized(this.plant));
        }
    }

    public BlockState getPlant() {
        return plant;
    }

    public void setPlant(BlockState blockState) {
        this.plant = blockState == null ? BlockStates.AIR : blockState;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
