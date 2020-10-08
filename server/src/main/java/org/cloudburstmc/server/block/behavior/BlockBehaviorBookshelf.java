package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorBookshelf extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                ItemStack.get(ItemTypes.BOOK, 3)
        };
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }


}
