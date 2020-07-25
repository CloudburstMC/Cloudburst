package org.cloudburstmc.server.block;

public interface BlockSnapshot {

    default BlockState getState() {
        return getState(0);
    }

    default BlockState getExtra() {
        return getState(1);
    }

    BlockState getState(int layer);
}
