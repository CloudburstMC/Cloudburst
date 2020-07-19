package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created on 2015/12/2 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorWheat extends BlockBehaviorCrops {

    public BlockBehaviorWheat(Identifier id) {
        super(id);
    }

    @Override
    public Item toItem() {
        return Item.get(ItemIds.WHEAT);
    }

    @Override
    public Item[] getDrops(Item hand) {
        if (this.getMeta() >= 0x07) {
            return new Item[]{
                    Item.get(ItemIds.WHEAT),
                    Item.get(ItemIds.WHEAT, 0, (int) (4f * Math.random()))
            };
        } else {
            return new Item[]{
                    Item.get(ItemIds.WHEAT)
            };
        }
    }
}
