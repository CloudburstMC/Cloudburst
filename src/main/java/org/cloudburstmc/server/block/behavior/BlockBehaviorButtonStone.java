package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by CreeperFace on 27. 11. 2016.
 */
public class BlockBehaviorButtonStone extends BlockBehaviorButton {

    public BlockBehaviorButtonStone(Identifier id) {
        super(id);
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public Item[] getDrops(Item hand) {
        if (hand.isPickaxe()) {
            return super.getDrops(hand);
        } else {
            return new Item[0];
        }
    }
}
