package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.Bed;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.data.BedData;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3i;

public class BedBlockEntity extends BaseBlockEntity implements Bed {

    public DyeColor color = DyeColor.WHITE;

    public BedBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        this.color = DyeColor.getByWoolData(((BedData) data).getColor());
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        ((BedData) data).setColor((byte) this.getColor().getWoolData());
    }

    @Override
    public DyeColor getColor() {
        return color;
    }

    @Override
    public void setColor(DyeColor color) {
        this.color = color;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
