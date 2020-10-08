package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.math.AxisAlignedBB;

public class BlockBehaviorLight extends BlockBehaviorTransparent {

    @Override
    public int getLightLevel(Block block) {
        return block.getState().ensureTrait(BlockTraits.LIGHT_LEVEL);
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[0];
    }
}
