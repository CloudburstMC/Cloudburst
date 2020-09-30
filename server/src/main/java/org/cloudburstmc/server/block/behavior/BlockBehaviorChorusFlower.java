package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;

public class BlockBehaviorChorusFlower extends BlockBehaviorTransparent {


    @Override
    public float getResistance() {
        return 2;
    }

    @Override
    public ToolType getToolType(BlockState state) {
        return ItemToolBehavior.TYPE_NONE;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[0];
    }
}
