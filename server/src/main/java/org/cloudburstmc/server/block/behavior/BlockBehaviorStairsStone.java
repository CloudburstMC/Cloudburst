package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorStairsStone extends BlockBehaviorStairs {

    @Override
    public float getHardness() {
        return 1.5f;
    }

    @Override
    public float getResistance() {
        return 10;
    }

    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.STONE_BLOCK_COLOR;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}
