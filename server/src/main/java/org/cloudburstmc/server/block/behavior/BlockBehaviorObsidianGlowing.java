package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;

import static org.cloudburstmc.server.block.BlockIds.OBSIDIAN;

public class BlockBehaviorObsidianGlowing extends BlockBehaviorSolid {

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public float getHardness() {
        return 50;
    }

    @Override
    public float getResistance() {
        return 6000;
    }

    @Override
    public int getLightLevel(Block block) {
        return 12;
    }

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

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}
