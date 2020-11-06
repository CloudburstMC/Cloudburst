package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;

public class BlockBehaviorMobSpawner extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block blockState, ItemStack hand) {
        return new ItemStack[0];
    }

    @Override
    public boolean canBePushed() {
        return false;
    }


}
