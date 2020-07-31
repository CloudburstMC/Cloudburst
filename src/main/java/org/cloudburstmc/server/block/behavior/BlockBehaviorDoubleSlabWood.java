package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemTool;

import static org.cloudburstmc.server.block.BlockTypes.WOODEN_SLAB;

public class BlockBehaviorDoubleSlabWood extends BlockBehaviorDoubleSlab {

    public BlockBehaviorDoubleSlabWood() {
        super(WOODEN_SLAB, BlockTraits.WOOD_TYPE);
    }

    @Override
    public float getResistance() {
        return 15;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
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
    public boolean canHarvestWithHand() {
        return true;
    }
}
