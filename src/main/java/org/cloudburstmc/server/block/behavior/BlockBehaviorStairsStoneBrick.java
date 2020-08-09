package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.ItemTool;

public class BlockBehaviorStairsStoneBrick extends BlockBehaviorStairs {

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public float getHardness() {
        return 1.5f;
    }

    @Override
    public float getResistance() {
        return 30;
    }
}
