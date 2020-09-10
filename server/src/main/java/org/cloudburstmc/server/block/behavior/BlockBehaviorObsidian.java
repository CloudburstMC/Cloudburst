package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockIds.PORTAL;

public class BlockBehaviorObsidian extends BlockBehaviorSolid {

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public float getHardness() {
        return 35; //50 in PC
    }

    @Override
    public float getResistance() {
        return 6000;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemToolBehavior.TIER_DIAMOND) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        //destroy the nether portal
        for (Direction direction : Direction.values()) {
            val b = block.getSide(direction);
            val state = b.getState();
            if (state.getType() == PORTAL) {
                state.getBehavior().onBreak(b, item);
            }
        }

        return super.onBreak(block, item);
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.OBSIDIAN_BLOCK_COLOR;
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
