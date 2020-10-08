package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;

import java.util.Random;

public class BlockBehaviorGravel extends BlockBehaviorFallable {


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (new Random().nextInt(9) == 0) {
            return new ItemStack[]{
                    ItemStack.get(ItemTypes.FLINT)
            };
        } else {
            return new ItemStack[]{
                    toItem(block)
            };
        }
    }
}
