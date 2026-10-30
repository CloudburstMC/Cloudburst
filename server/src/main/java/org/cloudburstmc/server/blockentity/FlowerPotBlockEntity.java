package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.FlowerPot;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.FlowerPotData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;


public class FlowerPotBlockEntity extends BaseBlockEntity implements FlowerPot {

    private BlockState plant;

    public FlowerPotBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return FlowerPotData.class;
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        this.plant = ((FlowerPotData) data).getPlant();
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        ((FlowerPotData) data).setPlant(this.plant);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("item") || tag.containsKey("mData")) {
            throw new UnsupportedOperationException("Numeric flower pot data is no longer supported");
        }
    }

    public BlockState getPlant() {
        return plant != null ? plant : BlockStates.AIR;
    }

    public void setPlant(BlockState blockState) {
        this.plant = blockState;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
