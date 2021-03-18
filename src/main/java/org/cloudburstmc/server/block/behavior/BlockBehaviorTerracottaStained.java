package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.TierTypes;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.api.util.data.DyeColor;

public class BlockBehaviorTerracottaStained extends BlockBehaviorSolid {

    @Override
    public ItemStack[] getDrops(Block blockState, ItemStack hand) {
        val behavior = hand.getBehavior();
        if (behavior.isPickaxe() && behavior.getTier(hand).compareTo(TierTypes.WOOD) >= 0) {
            return new ItemStack[]{toItem(blockState)};
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        return getDyeColor(block.getState()).getColor();
    }

    public DyeColor getDyeColor(BlockState state) {
        return state.ensureTrait(BlockTraits.COLOR);
    }

}
