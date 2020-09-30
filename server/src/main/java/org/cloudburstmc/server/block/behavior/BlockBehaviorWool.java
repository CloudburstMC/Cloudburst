package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

public class BlockBehaviorWool extends BlockBehaviorSolid {

    @Override
    public ToolType getToolType(BlockState state) {
        return ItemToolBehavior.TYPE_SHEARS;
    }


    @Override
    public float getResistance() {
        return 4;
    }

    @Override
    public int getBurnChance(BlockState state) {
        return 30;
    }

    @Override
    public int getBurnAbility(BlockState state) {
        return 60;
    }

    @Override
    public BlockColor getColor(Block block) {
        return getDyeColor(block.getState()).getColor();
    }

    public DyeColor getDyeColor(BlockState state) {
        return state.ensureTrait(BlockTraits.COLOR);
    }
}
