package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.TierTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;

import static org.cloudburstmc.api.block.BlockTypes.PORTAL;

public class BlockBehaviorObsidian extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.getBehavior().isPickaxe() && hand.getBehavior().getTier(hand).compareTo(TierTypes.DIAMOND) >= 0) {
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
            var b = block.getSide(direction);
            var state = b.getState();
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


}
