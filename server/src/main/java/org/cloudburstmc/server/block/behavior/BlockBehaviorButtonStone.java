package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;

public class BlockBehaviorButtonStone extends BlockBehaviorButton {

    public BlockBehaviorButtonStone() {
        super(BlockIds.STONE_BUTTON);
    }

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe()) {
            return super.getDrops(block, hand);
        } else {
            return new ItemStack[0];
        }
    }
}
