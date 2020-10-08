package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;

import static org.cloudburstmc.server.block.BlockTypes.OBSIDIAN;

public class BlockBehaviorObsidianGlowing extends BlockBehaviorSolid {


    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(OBSIDIAN);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() > ItemToolBehavior.TIER_DIAMOND) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public boolean canBePushed() {
        return false;
    }


}
