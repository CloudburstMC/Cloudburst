package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Comparator;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.ComparatorData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;

public class ComparatorBlockEntity extends BaseBlockEntity implements Comparator {

    public ComparatorBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return ComparatorData.class;
    }

    @Override
    protected ComparatorData data() {
        return (ComparatorData) super.data();
    }

    public int getOutputSignal() {
        return data().getOutputSignal();
    }

    public void setOutputSignal(int outputSignal) {
        data().setOutputSignal(outputSignal);
    }
}
