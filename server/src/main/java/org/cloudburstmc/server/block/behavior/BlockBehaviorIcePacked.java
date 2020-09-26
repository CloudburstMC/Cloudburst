package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;

public class BlockBehaviorIcePacked extends BlockBehaviorIce {

    @Override
    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public int onUpdate(Block block, int type) {
        return 0; //not being melted
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        return removeBlock(block);
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}
