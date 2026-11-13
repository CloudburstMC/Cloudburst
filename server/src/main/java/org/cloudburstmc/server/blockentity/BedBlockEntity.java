package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.Bed;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.blockentity.data.CloudBedData;

public class BedBlockEntity extends BaseBlockEntity implements Bed {

    private final CloudBedData persistenceState;

    public BedBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        CloudBedData state = new CloudBedData();
        super(type, chunk, position, state);
        this.persistenceState = state;
    }

    @Override
    protected CloudBedData data() {
        return this.persistenceState;
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
