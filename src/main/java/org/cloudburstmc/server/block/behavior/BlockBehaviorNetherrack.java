package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created on 2015/12/26 by Pub4Game.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorNetherrack extends BlockBehaviorSolid {

    public BlockBehaviorNetherrack(Identifier id) {
        super(id);
    }

    @Override
    public float getResistance() {
        return 2;
    }

    @Override
    public float getHardness() {
        return 0.4f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public Item[] getDrops(Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    toItem()
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.NETHERRACK_BLOCK_COLOR;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

}
