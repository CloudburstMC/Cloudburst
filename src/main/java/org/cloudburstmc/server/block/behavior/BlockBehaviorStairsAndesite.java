package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * @author Erik Miller | EinBexiii | Bex
 */
public class BlockBehaviorStairsAndesite extends BlockBehaviorStairs {

    public BlockBehaviorStairsAndesite(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 1.5f;
    }

    @Override
    public float getResistance() {
        return 30;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.STONE_BLOCK_COLOR;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}
