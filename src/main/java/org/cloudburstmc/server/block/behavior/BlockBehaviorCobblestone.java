package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.Block;

public class BlockBehaviorCobblestone extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (checkTool(block.getState(), hand)) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }


}
