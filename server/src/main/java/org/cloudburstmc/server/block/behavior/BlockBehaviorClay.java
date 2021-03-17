package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.item.ItemTypes;

public class BlockBehaviorClay extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                CloudItemRegistry.get().getItem(ItemTypes.CLAY_BALL, 4)
        };
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.CLAY_BLOCK_COLOR;
    }


}
