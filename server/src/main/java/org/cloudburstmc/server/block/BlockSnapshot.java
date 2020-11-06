package org.cloudburstmc.server.block;

public interface BlockSnapshot {

    default BlockState getState() {
        return getState(0);
    }

    default BlockState getExtra() {
        return getState(1);
    }

    BlockState getState(int layer);

    default int getLiquidLayer() {
        BlockState state = getExtra();
        if (state == BlockStates.AIR) {
            return 0;
        }
        return 1;
    }

    default BlockState getLiquid() {
        BlockState state = getExtra();
        if (state == BlockStates.AIR) {
            return getState();
        }
        return state;
    }
}
