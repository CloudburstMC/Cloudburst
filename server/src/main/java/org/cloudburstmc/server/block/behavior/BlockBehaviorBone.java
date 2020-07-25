package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorBone extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 2;
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
    public Item[] getDrops(BlockState blockState, Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{Item.get(id)};
        }

        return new Item[0];
    }

    @Override
    public Direction getBlockFace() {
        return Direction.fromHorizontalIndex(this.getMeta() & 0x7);
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.SAND_BLOCK_COLOR;
    }
}
