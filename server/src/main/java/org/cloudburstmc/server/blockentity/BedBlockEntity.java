package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.Bed;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.data.BedData;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3i;

public class BedBlockEntity extends BaseBlockEntity implements Bed {

    public BedBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return BedData.class;
    }

    @Override
    protected BedData data() {
        return (BedData) super.data();
    }

    @Override
    public DyeColor getColor() {
        return data().getColor();
    }

    @Override
    public void setColor(DyeColor color) {
        data().setColor(color);
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
