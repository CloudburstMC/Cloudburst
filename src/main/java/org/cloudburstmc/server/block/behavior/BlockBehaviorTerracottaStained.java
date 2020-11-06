package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.TierTypes;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

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
