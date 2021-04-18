package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.server.level.CloudLevel;

public class BlockBehaviorWallBanner extends BlockBehaviorBanner {

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            var side = block.getSide(block.getState().ensureTrait(BlockTraits.FACING_DIRECTION).getOpposite());
            if (side.getState() == BlockStates.AIR) {
                block.getLevel().useBreakOn(block.getPosition());
            }

            return CloudLevel.BLOCK_UPDATE_NORMAL;
        }
        return 0;
    }
}
