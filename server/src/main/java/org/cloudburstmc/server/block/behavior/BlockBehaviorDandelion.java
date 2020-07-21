package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;

import static org.cloudburstmc.server.block.BlockTypes.RED_FLOWER;

public class BlockBehaviorDandelion extends BlockBehaviorFlower {

    @Override
    protected BlockState getUncommonFlower() {
        return BlockState.get(RED_FLOWER);
    }
}
