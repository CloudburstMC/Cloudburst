package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorSand extends BlockBehaviorFallable {


    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public ToolType getToolType(BlockState state) {
        return ItemToolBehavior.TYPE_SHOVEL;
    }

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.SAND_BLOCK_COLOR;
    }
}
