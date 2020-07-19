package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * @author Erik Miller | EinBexiii | Bex
 */
public class BlockBehaviorStairsSmoothRedSandstone extends BlockBehaviorStairs {

    public BlockBehaviorStairsSmoothRedSandstone(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 2.0f;
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
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.ORANGE_BLOCK_COLOR;
    }
}
