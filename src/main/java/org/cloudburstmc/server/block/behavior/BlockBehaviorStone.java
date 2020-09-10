package org.cloudburstmc.server.block.behavior;

import lombok.var;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.utils.data.StoneType;

import static org.cloudburstmc.server.block.BlockIds.COBBLESTONE;

public class BlockBehaviorStone extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 1.5f;
    }

    @Override
    public float getResistance() {
        return 10;
    }

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemToolBehavior.TIER_WOODEN) {
            var state = block.getState();
            if (state.ensureTrait(BlockTraits.STONE_TYPE) == StoneType.STONE) {
                state = BlockState.get(COBBLESTONE);
            }
            return new ItemStack[]{ItemStack.get(state)};
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}