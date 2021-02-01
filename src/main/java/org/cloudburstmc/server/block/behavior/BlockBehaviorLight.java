package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;

public class BlockBehaviorLight extends BlockBehaviorTransparent {

    @Override
    public int getLightLevel(Block block) {
        return block.getState().ensureTrait(BlockTraits.LIGHT_LEVEL);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[0];
    }
}
