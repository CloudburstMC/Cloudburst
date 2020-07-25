package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;

public class BlockBehaviorWheat extends BlockBehaviorCrops {

    @Override
    public Item toItem(BlockState state) {
        return Item.get(ItemIds.WHEAT);
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
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
