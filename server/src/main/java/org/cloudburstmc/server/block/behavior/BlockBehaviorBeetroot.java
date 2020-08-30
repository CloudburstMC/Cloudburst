package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;

import static org.cloudburstmc.server.item.behavior.ItemIds.BEETROOT_SEEDS;

public class BlockBehaviorBeetroot extends BlockBehaviorCrops {

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.BEETROOT_SEEDS);
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (hand.getMeta() >= 0x07) {
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
