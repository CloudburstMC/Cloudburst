package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created on 2015/11/25 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorStairsWood extends BlockBehaviorStairs {
    public BlockBehaviorStairsWood(Identifier id) {
        super(id);
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(id, 0);
    }

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public float getResistance() {
        return 15;
    }

    @Override
    public int getBurnChance() {
        return 5;
    }

    @Override
    public int getBurnAbility() {
        return 20;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.WOOD_BLOCK_COLOR;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[]{
                toItem(blockState)
        };
    }
}
