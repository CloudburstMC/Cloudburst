package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorGlass extends BlockBehaviorTransparent {

    @Override
    public float getResistance() {
        return 1.5f;
    }

    @Override
    public float getHardness() {
        return 0.3f;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[0];
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.AIR_BLOCK_COLOR;
    }
}
