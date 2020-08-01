package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.level.Level;

public class BlockBehaviorLavaStill extends BlockBehaviorLava {

    @Override
    public BlockState getBlock(int level) {
        return BlockState.get(BlockTypes.LAVA);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type != Level.BLOCK_UPDATE_SCHEDULED) {
            return super.onUpdate(block, type);
        }
        return 0;
    }
}
