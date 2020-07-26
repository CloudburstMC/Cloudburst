package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;

import java.util.Random;

public class BlockBehaviorPotato extends BlockBehaviorCrops {

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.POTATO);
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (getMeta() >= 0x07) {
            return new Item[]{
                    Item.get(ItemIds.POTATO, 0, new Random().nextInt(3) + 1)
            };
        } else {
            return new Item[]{
                    Item.get(ItemIds.POTATO)
            };
        }
    }
}
