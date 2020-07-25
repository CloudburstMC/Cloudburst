package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorRedstone extends BlockBehaviorSolid {

    @Override
    public float getResistance() {
        return 10;
    }

    @Override
    public float getHardness() {
        return 5;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    //TODO: redstone

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
        return BlockColor.REDSTONE_BLOCK_COLOR;
    }

    @Override
    public boolean isPowerSource() {
        return true;
    }

    @Override
    public int getWeakPower(Direction face) {
        return 15;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}