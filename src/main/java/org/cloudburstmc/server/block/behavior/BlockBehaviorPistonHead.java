package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStacks;

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
