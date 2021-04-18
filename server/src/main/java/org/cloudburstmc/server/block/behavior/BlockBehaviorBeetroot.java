package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.item.ItemTypes.BEETROOT;
import static org.cloudburstmc.api.item.ItemTypes.BEETROOT_SEEDS;

public class BlockBehaviorBeetroot extends BlockBehaviorCrops {

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(BEETROOT_SEEDS);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (block.getState().ensureTrait(BlockTraits.GROWTH) >= 0x07) {
            return new ItemStack[]{
                    CloudItemRegistry.get().getItem(BEETROOT),
                    CloudItemRegistry.get().getItem(BEETROOT_SEEDS, (int) (4f * Math.random()))
            };
        } else {
            return new ItemStack[]{
                    CloudItemRegistry.get().getItem(BEETROOT_SEEDS)
            };
        }
    }
}
