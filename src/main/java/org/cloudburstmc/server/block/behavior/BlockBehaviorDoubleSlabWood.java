package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;

import static org.cloudburstmc.server.block.BlockTypes.WOODEN_SLAB;

public class BlockBehaviorDoubleSlabWood extends BlockBehaviorDoubleSlab {

    public BlockBehaviorDoubleSlabWood() {
        super(WOODEN_SLAB, BlockTraits.TREE_SPECIES);
    }

    @Override
    public float getResistance() {
        return 15;
    }

    @Override
    public ToolType getToolType(BlockState state) {
        return ItemToolBehavior.TYPE_AXE;
    }

    @Override
    public int getBurnChance(BlockState state) {
        return 5;
    }

    @Override
    public int getBurnAbility(BlockState state) {
        return 20;
    }

    @Override
    public boolean canHarvestWithHand() {
        return true;
    }
}
