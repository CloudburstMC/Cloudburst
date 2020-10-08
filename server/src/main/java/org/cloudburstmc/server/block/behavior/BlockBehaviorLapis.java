package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorLapis extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemToolBehavior.TIER_STONE) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.LAPIS_BLOCK_COLOR;
    }


}
