package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DyeColor;

/**
 * Created on 2015/12/2 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorTerracottaStained extends BlockBehaviorSolid {

    public BlockBehaviorTerracottaStained(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 1.25f;
    }

    @Override
    public float getResistance() {
        return 0.75f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{toItem(blockState)};
        } else {
            return new Item[0];
        }
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return DyeColor.getByWoolData(getMeta()).getColor();
    }

    public DyeColor getDyeColor() {
        return DyeColor.getByWoolData(getMeta());
    }

}
