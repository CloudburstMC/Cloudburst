package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Random;

/**
 * @author Nukkit Project Team
 */
public class BlockBehaviorCarrot extends BlockBehaviorCrops {

    public BlockBehaviorCarrot(Identifier id) {
        super(id);
    }

    @Override
    public Item[] getDrops(Item hand) {
        if (getMeta() >= 0x07) {
            return new Item[]{
                    Item.get(ItemIds.CARROT, 0, new Random().nextInt(3) + 1)
            };
        }
        return new Item[]{
                Item.get(ItemIds.CARROT)
        };
    }

    @Override
    public Item toItem() {
        return Item.get(ItemIds.CARROT);
    }
}
