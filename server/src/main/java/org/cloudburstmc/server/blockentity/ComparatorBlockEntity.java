package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Comparator;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.blockentity.data.CloudBlockEntityData;
import org.cloudburstmc.server.blockentity.data.CloudComparatorData;

public class ComparatorBlockEntity extends BaseBlockEntity implements Comparator {

    public ComparatorBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends CloudBlockEntityData> minimumDataClass() {
        return CloudComparatorData.class;
    }

    @Override
    protected CloudComparatorData data() {
        return (CloudComparatorData) super.data();
    }

    public int getOutputSignal() {
        return data().getOutputSignal();
    }

    public void setOutputSignal(int outputSignal) {
        data().setOutputSignal(outputSignal);
    }
}
