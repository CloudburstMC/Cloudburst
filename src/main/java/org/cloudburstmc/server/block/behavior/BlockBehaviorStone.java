package org.cloudburstmc.server.block.behavior;

import lombok.val;
import lombok.var;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.item.TierTypes;
import org.cloudburstmc.server.utils.data.StoneType;

import static org.cloudburstmc.api.block.BlockTypes.COBBLESTONE;

public class BlockBehaviorStone extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        val behavior = hand.getBehavior();

        if (behavior.isPickaxe() && behavior.getTier(hand).compareTo(TierTypes.WOOD) >= 0) {
            var state = block.getState();
            if (state.ensureTrait(BlockTraits.STONE_TYPE) == StoneType.STONE) {
                state = BlockState.get(COBBLESTONE);
            }
            return new ItemStack[]{ItemStack.get(state)};
        } else {
            return new ItemStack[0];
        }
    }


}
