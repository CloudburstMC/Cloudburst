package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.TerracottaColor;

public class BlockBehaviorTerracotta extends BlockBehaviorSolid {

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public float getHardness() {
        return 1.25f;
    }

    @Override
    public float getResistance() {
        return 7;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    toItem(blockState)
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return TerracottaColor.getByTerracottaData(getMeta()).getColor();
    }

    public TerracottaColor getDyeColor() {
        return TerracottaColor.getByTerracottaData(getMeta());
    }
}
