package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Random;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockBehaviorGravel extends BlockBehaviorFallable {

    public BlockBehaviorGravel(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 0.6f;
    }

    @Override
    public float getResistance() {
        return 3;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHOVEL;
    }

    @Override
    public Item[] getDrops(Item hand) {
        if (new Random().nextInt(9) == 0) {
            return new Item[]{
                    Item.get(ItemIds.FLINT)
            };
        } else {
            return new Item[]{
                    toItem()
            };
        }
    }
}
