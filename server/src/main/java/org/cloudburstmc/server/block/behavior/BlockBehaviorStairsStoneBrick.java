package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;

public class BlockBehaviorStairsStoneBrick extends BlockBehaviorStairs {

    @Override
    public ToolType getToolType(BlockState state) {
        return ItemToolBehavior.TYPE_PICKAXE;
    }


    @Override
    public float getResistance() {
        return 30;
    }
}
