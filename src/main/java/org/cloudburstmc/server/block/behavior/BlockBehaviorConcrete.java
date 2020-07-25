package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

public class BlockBehaviorConcrete extends BlockBehaviorSolid {

    @Override
    public float getResistance() {
        return 9;
    }

    @Override
    public float getHardness() {
        return 1.8f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return hand.getTier() >= ItemTool.TIER_WOODEN ? new Item[]{toItem(blockState)} : new Item[0];
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return DyeColor.getByWoolData(getMeta()).getColor();
    }

    public DyeColor getDyeColor() {
        return DyeColor.getByWoolData(getMeta());
    }
}
