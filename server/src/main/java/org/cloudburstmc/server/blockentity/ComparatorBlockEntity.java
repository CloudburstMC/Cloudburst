package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Comparator;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.ComparatorData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;

public class ComparatorBlockEntity extends BaseBlockEntity implements Comparator {

    private int outputSignal;

    public ComparatorBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return ComparatorData.class;
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        this.outputSignal = ((ComparatorData) data).getOutputSignal();
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        ((ComparatorData) data).setOutputSignal(this.outputSignal);
    }

    public int getOutputSignal() {
        return outputSignal;
    }

    public void setOutputSignal(int outputSignal) {
        this.outputSignal = outputSignal;
    }
}
