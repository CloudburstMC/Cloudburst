package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;

import static org.cloudburstmc.server.item.ItemIds.BEETROOT_SEEDS;

public class BlockBehaviorBeetroot extends BlockBehaviorCrops {

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemIds.BEETROOT_SEEDS);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.getMeta() >= 0x07) {
            return new ItemStack[]{
                    ItemStack.get(ItemIds.BEETROOT, 0, 1),
                    ItemStack.get(BEETROOT_SEEDS, 0, (int) (4f * Math.random()))
            };
        } else {
            return new ItemStack[]{
                    ItemStack.get(BEETROOT_SEEDS, 0, 1)
            };
        }
    }
}
