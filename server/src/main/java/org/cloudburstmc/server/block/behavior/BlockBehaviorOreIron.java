package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;

import static org.cloudburstmc.server.block.BlockTypes.IRON_ORE;

public class BlockBehaviorOreIron extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemToolBehavior.TIER_STONE) {
            return new ItemStack[]{
                    ItemStack.get(IRON_ORE)
            };
        } else {
            return new ItemStack[0];
        }
    }


}
