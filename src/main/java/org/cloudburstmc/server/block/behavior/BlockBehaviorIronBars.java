package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorIronBars extends BlockBehaviorThin {

    @Override
    public float getHardness() {
        return 5;
    }

    @Override
    public float getResistance() {
        return 10;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(block.getState().defaultState());
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    this.toItem(block)
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.IRON_BLOCK_COLOR;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
