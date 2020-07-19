package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DyeColor;

/**
 * Created by CreeperFace on 2.6.2017.
 */
public class BlockBehaviorConcrete extends BlockBehaviorSolid {

    public BlockBehaviorConcrete(Identifier id) {
        super(id);
    }

    @Override
    public float getResistance() {
        return 9;
    }

    @Override
    public float getHardness() {
        return 1.8f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public Item[] getDrops(Item hand) {
        return hand.getTier() >= ItemTool.TIER_WOODEN ? new Item[]{toItem()} : new Item[0];
    }

    @Override
    public BlockColor getColor() {
        return DyeColor.getByWoolData(getMeta()).getColor();
    }

    public DyeColor getDyeColor() {
        return DyeColor.getByWoolData(getMeta());
    }
}
