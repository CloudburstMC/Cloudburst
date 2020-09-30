package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;

public class BlockBehaviorMobSpawner extends BlockBehaviorSolid {

    @Override
    public ToolType getToolType(BlockState state) {
        return ItemToolBehavior.TYPE_PICKAXE;
    }


    @Override
    public float getResistance() {
        return 25;
    }

    @Override
    public ItemStack[] getDrops(Block blockState, ItemStack hand) {
        return new ItemStack[0];
    }

    @Override
    public boolean canBePushed() {
        return false;
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
