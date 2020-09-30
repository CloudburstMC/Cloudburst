package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;

import static org.cloudburstmc.server.block.BlockTypes.IRON_ORE;

public class BlockBehaviorOreIron extends BlockBehaviorSolid {



    @Override
    public float getResistance() {
        return 5;
    }

    @Override
    public ToolType getToolType(BlockState state) {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

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

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}
