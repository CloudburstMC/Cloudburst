package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;

public class BlockBehaviorAir extends BlockBehaviorTransparent {

    private static final ItemStack[] EMPTY = new ItemStack[0];

    @Override
    public boolean canBePlaced() {
        return false;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return EMPTY;
    }

    @Override
    public boolean canBeReplaced(Block block) {
        return true;
    }
}
