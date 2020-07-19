package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockTypes.WOODEN_SLAB;

/**
 * Created on 2015/12/2 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorDoubleSlabWood extends BlockBehaviorDoubleSlab {

    public BlockBehaviorDoubleSlabWood(Identifier id) {
        super(id, WOODEN_SLAB, BlockBehaviorSlabWood.COLORS);
    }

    @Override
    public float getResistance() {
        return 15;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public int getBurnChance() {
        return 5;
    }

    @Override
    public int getBurnAbility() {
        return 20;
    }

    @Override
    public boolean canHarvestWithHand() {
        return true;
    }

    @Override
    public Item[] getDrops(Item hand) {
        return new Item[]{
                Item.get(this.getSlabId(), this.getMeta() & 0x07, 2)
        };
    }
}
