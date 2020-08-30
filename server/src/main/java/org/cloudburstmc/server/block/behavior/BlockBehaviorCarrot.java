package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;

import java.util.Random;

public class BlockBehaviorCarrot extends BlockBehaviorCrops {

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (block.getState().ensureTrait(BlockTraits.GROWTH) >= 0x07) {
            return new Item[]{
                    Item.get(ItemIds.CARROT, 0, new Random().nextInt(3) + 1)
            };
        }
        return new Item[]{
                Item.get(ItemIds.CARROT)
        };
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.CARROT);
    }
}
