package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorClay extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 0.6f;
    }

    @Override
    public float getResistance() {
        return 3;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHOVEL;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[]{
                Item.get(ItemIds.CLAY_BALL, 0, 4)
        };
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.CLAY_BLOCK_COLOR;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}
