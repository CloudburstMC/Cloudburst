package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.item.ItemTypes;

public class BlockBehaviorBookshelf extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                CloudItemRegistry.get().getItem(ItemTypes.BOOK, 3)
        };
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }


}
