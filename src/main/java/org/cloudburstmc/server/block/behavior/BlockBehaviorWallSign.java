package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.level.Level;

public class BlockBehaviorWallSign extends BlockBehaviorSignPost {

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            val side = block.getSide(block.getState().ensureTrait(BlockTraits.FACING_DIRECTION).getOpposite());

            if (side.getState() == BlockStates.AIR) {
                block.getLevel().useBreakOn(block.getPosition());
            }
            return Level.BLOCK_UPDATE_NORMAL;
        }
        return 0;
    }
}
