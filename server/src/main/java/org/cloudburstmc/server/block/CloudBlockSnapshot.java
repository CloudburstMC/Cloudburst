package org.cloudburstmc.server.block;

import lombok.RequiredArgsConstructor;

import static com.google.common.base.Preconditions.checkElementIndex;

@RequiredArgsConstructor
public class CloudBlockSnapshot implements BlockSnapshot {
    private final BlockState[] states;

    @Override
    public BlockState getState(int layer) {
        checkElementIndex(layer, states.length);
        return states[layer];
    }
}
