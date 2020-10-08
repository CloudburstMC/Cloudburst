package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;

import static org.cloudburstmc.server.block.BlockTypes.GOLD_ORE;

public class BlockBehaviorOreGold extends BlockBehaviorSolid {

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (checkTool(block.getState(), hand)) {
            return new ItemStack[]{
                    ItemStack.get(GOLD_ORE)
            };
        } else {
            return new ItemStack[0];
        }
    }


}
