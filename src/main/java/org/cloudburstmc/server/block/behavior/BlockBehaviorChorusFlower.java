package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.Block;

public class BlockBehaviorChorusFlower extends BlockBehaviorTransparent {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[0];
    }
}
