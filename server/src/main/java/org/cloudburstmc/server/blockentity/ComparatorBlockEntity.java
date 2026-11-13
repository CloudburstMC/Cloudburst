package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Comparator;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.blockentity.data.CloudComparatorData;

public class ComparatorBlockEntity extends BaseBlockEntity implements Comparator {

    private final CloudComparatorData persistenceState;

    public ComparatorBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        CloudComparatorData state = new CloudComparatorData();
        super(type, chunk, position, state);
        this.persistenceState = state;
    }

    @Override
    protected CloudComparatorData data() {
        return this.persistenceState;
    }

    public int getOutputSignal() {
        return data().getOutputSignal();
    }

    public void setOutputSignal(int outputSignal) {
        data().setOutputSignal(outputSignal);
    }
}
