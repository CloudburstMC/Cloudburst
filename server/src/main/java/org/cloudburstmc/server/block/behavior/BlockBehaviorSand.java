package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorSand extends BlockBehaviorFallable {

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_SHOVEL;
    }

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.SAND_BLOCK_COLOR;
    }
}
