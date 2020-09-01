package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.behavior.Item;
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
    public Item[] getDrops(Block block, Item hand) {
        return new Item[]{
                toItem(block)
        };
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.RED_BLOCK_COLOR;
    }
}
