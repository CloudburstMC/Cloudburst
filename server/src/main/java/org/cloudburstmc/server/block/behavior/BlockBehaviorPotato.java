package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Random;

/**
 * Created by Pub4Game on 15.01.2016.
 */
public class BlockBehaviorPotato extends BlockBehaviorCrops {

    public BlockBehaviorPotato(Identifier id) {
        super(id);
    }

    @Override
    public Item toItem() {
        return Item.get(ItemIds.POTATO);
    }

    @Override
    public Item[] getDrops(Item hand) {
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
