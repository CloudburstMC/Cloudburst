package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;

import static org.cloudburstmc.server.item.ItemIds.BEETROOT_SEEDS;

public class BlockBehaviorBeetroot extends BlockBehaviorCrops {

    @Override
    public Item toItem(BlockState state) {
        return Item.get(ItemIds.BEETROOT_SEEDS);
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        if (this.getMeta() >= 0x07) {
            return new Item[]{
                    Item.get(ItemIds.BEETROOT, 0, 1),
                    Item.get(BEETROOT_SEEDS, 0, (int) (4f * Math.random()))
            };
        } else {
            return new Item[]{
                    Item.get(BEETROOT_SEEDS, 0, 1)
            };
        }
    }
}
