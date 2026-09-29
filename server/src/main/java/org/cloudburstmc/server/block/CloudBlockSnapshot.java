package org.cloudburstmc.server.block;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockSnapshot;
import org.cloudburstmc.api.block.BlockState;

@RequiredArgsConstructor
@ToString
public class CloudBlockSnapshot implements BlockSnapshot {
    private final BlockState[] states;

    @Override
    public BlockState getState(BlockLayer layer) {
        return this.states[switch (layer) {
            case PRIMARY -> 0;
            case SECONDARY -> 1;
        }];
    }
}
