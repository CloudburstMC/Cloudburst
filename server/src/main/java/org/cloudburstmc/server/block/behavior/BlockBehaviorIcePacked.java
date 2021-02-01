package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.Block;

public class BlockBehaviorIcePacked extends BlockBehaviorIce {


    @Override
    public int onUpdate(Block block, int type) {
        return 0; //not being melted
    }


    @Override
    public boolean onBreak(Block block, ItemStack item) {
        return removeBlock(block);
    }


}
