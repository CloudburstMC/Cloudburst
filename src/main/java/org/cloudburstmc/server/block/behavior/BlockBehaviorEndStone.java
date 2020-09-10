package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorEndStone extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 3;
    }

    @Override
    public float getResistance() {
        return 45;
    }

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemToolBehavior.TIER_WOODEN) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.SAND_BLOCK_COLOR;
    }
}