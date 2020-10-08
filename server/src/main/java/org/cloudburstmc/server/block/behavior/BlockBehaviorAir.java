package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorAir extends BlockBehaviorTransparent {

    private static final ItemStack[] EMPTY = new ItemStack[0];

    @Override
    public boolean canBePlaced() {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return EMPTY;
    }
}
