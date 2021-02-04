package org.cloudburstmc.api.blockentity;

public interface Comparator extends BlockEntity {

    int getOutputSignal();

    void setOutputSignal(int outputSignal);
}
