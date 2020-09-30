package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;

public class BlockBehaviorSmoothStone extends BlockBehaviorSolid {


    @Override
    public float getResistance() {
        return 10;
    }

    @Override
    public ToolType getToolType(BlockState state) {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe()) {
            return new ItemStack[]{toItem(block)};
        } else {
            return new ItemStack[0];
        }
    }

}