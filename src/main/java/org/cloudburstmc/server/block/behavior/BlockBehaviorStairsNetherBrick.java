package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created on 2015/11/25 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorStairsNetherBrick extends BlockBehaviorStairs {
    public BlockBehaviorStairsNetherBrick(Identifier id) {
        super(id);
    }

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
    public BlockColor getColor(BlockState state) {
        return BlockColor.NETHERRACK_BLOCK_COLOR;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}
