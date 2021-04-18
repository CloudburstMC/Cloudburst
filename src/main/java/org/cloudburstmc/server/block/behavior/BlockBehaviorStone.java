package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.TierTypes;
import org.cloudburstmc.api.util.data.StoneType;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.block.BlockTypes.COBBLESTONE;

public class BlockBehaviorStone extends BlockBehaviorSolid {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        var behavior = hand.getBehavior();

        if (behavior.isPickaxe() && behavior.getTier(hand).compareTo(TierTypes.WOOD) >= 0) {
            var state = block.getState();
            if (state.ensureTrait(BlockTraits.STONE_TYPE) == StoneType.STONE) {
                state = CloudBlockRegistry.get().getBlock(COBBLESTONE);
            }
            return new ItemStack[]{CloudItemRegistry.get().getItem(state)};
        } else {
            return new ItemStack[0];
        }
    }


}
