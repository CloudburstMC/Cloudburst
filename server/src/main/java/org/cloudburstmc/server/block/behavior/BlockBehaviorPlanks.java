package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.util.data.BlockColor;

public class BlockBehaviorPlanks extends BlockBehaviorSolid {


    @Override
    public BlockColor getColor(Block block) {
        return switch (block.getState().ensureTrait(BlockTraits.TREE_SPECIES)) {
            case OAK -> BlockColor.WOOD_BLOCK_COLOR;
            case SPRUCE -> BlockColor.SPRUCE_BLOCK_COLOR;
            case BIRCH -> BlockColor.SAND_BLOCK_COLOR;
            case JUNGLE -> BlockColor.DIRT_BLOCK_COLOR;
            case ACACIA -> BlockColor.ORANGE_BLOCK_COLOR;
            case DARK_OAK -> BlockColor.BROWN_BLOCK_COLOR;
            case MANGROVE -> BlockColor.RED_BLOCK_COLOR;
            default -> BlockColor.WOOD_BLOCK_COLOR;
        };
    }
}
