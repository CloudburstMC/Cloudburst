package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

public class BlockNetherWartBlockBehavior extends BlockBehaviorSolid {

    public BlockNetherWartBlockBehavior(Identifier id) {
        super(id);
    }

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
    public BlockColor getColor() {
        return BlockColor.RED_BLOCK_COLOR;
    }
}
