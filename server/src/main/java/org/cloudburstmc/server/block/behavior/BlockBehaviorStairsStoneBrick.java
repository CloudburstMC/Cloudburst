package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.behavior.ItemToolBehavior;

public class BlockBehaviorStairsStoneBrick extends BlockBehaviorStairs {

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
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
