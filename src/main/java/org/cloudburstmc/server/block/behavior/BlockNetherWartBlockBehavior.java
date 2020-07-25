package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockNetherWartBlockBehavior extends BlockBehaviorSolid {

    @Override
    public float getResistance() {
        return 5;
    }

    @Override
    public float getHardness() {
        return 1;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[]{
                toItem(blockState)
        };
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.RED_BLOCK_COLOR;
    }
}
