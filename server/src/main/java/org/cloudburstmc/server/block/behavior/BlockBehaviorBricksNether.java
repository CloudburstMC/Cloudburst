package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockIds.NETHER_BRICK;

public class BlockBehaviorBricksNether extends BlockBehaviorSolid {

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public float getResistance() {
        return 10;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemToolBehavior.TIER_WOODEN) {
            return new ItemStack[]{
                    ItemStack.get(NETHER_BRICK, 0, 1)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.NETHERRACK_BLOCK_COLOR;
    }
}
