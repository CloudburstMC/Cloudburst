package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockHoneycombBlockBehavior extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 0.6f;
    }

    @Override
    public float getResistance() {
        return 3;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_HANDS_ONLY;
    }

    @Override
    public boolean canHarvestWithHand() {
        return true;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.ORANGE_BLOCK_COLOR;
    }
}
