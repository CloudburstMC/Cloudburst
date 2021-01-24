package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.world.World;

public class BlockBehaviorWallBanner extends BlockBehaviorBanner {

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL) {
            val side = block.getSide(block.getState().ensureTrait(BlockTraits.FACING_DIRECTION).getOpposite());
            if (side.getState() == BlockStates.AIR) {
                block.getWorld().useBreakOn(block.getPosition());
            }

            return World.BLOCK_UPDATE_NORMAL;
        }
        return 0;
    }
}
