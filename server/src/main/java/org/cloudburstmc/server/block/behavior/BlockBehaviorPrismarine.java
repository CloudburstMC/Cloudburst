package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorPrismarine extends BlockBehaviorSolid {

    @Override
    public BlockColor getColor(Block block) {
        switch (block.getState().ensureTrait(BlockTraits.PRISMARINE_BLOCK_TYPE)) {
            case DEFAULT:
                return BlockColor.CYAN_BLOCK_COLOR;
            case BRICKS:
            case DARK:
                return BlockColor.DIAMOND_BLOCK_COLOR;
            default:
                return BlockColor.STONE_BLOCK_COLOR;
        }
    }
}
