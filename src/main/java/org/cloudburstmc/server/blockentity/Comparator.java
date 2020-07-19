package org.cloudburstmc.server.blockentity;

public interface Comparator extends BlockEntity {

    int getOutputSignal();

    void setOutputSignal(int outputSignal);
}
