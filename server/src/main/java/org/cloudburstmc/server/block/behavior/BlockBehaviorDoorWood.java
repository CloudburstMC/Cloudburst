package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorDoorWood extends BlockBehaviorDoor {

    @Override
    public float getHardness() {
        return 3;
    }

    @Override
    public float getResistance() {
        return 15;
    }

    @Override
    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_AXE;
    }

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }
}
