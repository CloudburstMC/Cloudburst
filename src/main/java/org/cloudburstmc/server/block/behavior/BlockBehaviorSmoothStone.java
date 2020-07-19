package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.Identifier;

public class BlockBehaviorSmoothStone extends BlockBehaviorSolid {

    public BlockBehaviorSmoothStone(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 1.5f;
    }

    @Override
    public float getResistance() {
        return 10;
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
            return new Item[]{toItem()};
        } else {
            return new Item[0];
        }
    }

}