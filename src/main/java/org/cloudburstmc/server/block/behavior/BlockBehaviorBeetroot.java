package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.item.ItemIds.BEETROOT_SEEDS;

/**
 * Created on 2015/11/22 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorBeetroot extends BlockBehaviorCrops {
    public BlockBehaviorBeetroot(Identifier id) {
        super(id);
    }

    @Override
    public Item toItem() {
        return Item.get(ItemIds.BEETROOT_SEEDS);
    }

    @Override
    public Item[] getDrops(Item hand) {
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
