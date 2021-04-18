package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Random;

public class BlockBehaviorGravel extends BlockBehaviorFallable {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (new Random().nextInt(9) == 0) {
            return new ItemStack[]{
                    CloudItemRegistry.get().getItem(ItemTypes.FLINT)
            };
        } else {
            return new ItemStack[]{
                    toItem(block)
            };
        }
    }
}
