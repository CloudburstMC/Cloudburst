package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Random;

public class BlockBehaviorPotato extends BlockBehaviorCrops {

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(ItemTypes.POTATO);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (block.getState().ensureTrait(BlockTraits.GROWTH) == 7) {
            return new ItemStack[]{
                    CloudItemRegistry.get().getItem(ItemTypes.POTATO, new Random().nextInt(3) + 1)
            };
        } else {
            return new ItemStack[]{
                    CloudItemRegistry.get().getItem(ItemTypes.POTATO)
            };
        }
    }
}
