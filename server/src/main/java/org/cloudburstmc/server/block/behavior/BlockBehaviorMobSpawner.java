package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.Block;

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
