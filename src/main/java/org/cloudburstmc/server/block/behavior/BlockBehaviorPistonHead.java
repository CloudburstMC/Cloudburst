package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemStacks;

public class BlockBehaviorPistonHead extends BlockBehaviorTransparent {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[0];
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStacks.AIR;
    }


}
