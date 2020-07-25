package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

public class BlockBehaviorWool extends BlockBehaviorSolid {

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHEARS;
    }

    @Override
    public float getHardness() {
        return 0.8f;
    }

    @Override
    public float getResistance() {
        return 4;
    }

    @Override
    public int getBurnChance() {
        return 30;
    }

    @Override
    public int getBurnAbility() {
        return 60;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return DyeColor.getByWoolData(getMeta()).getColor();
    }

    public DyeColor getDyeColor() {
        return DyeColor.getByWoolData(getMeta());
    }
}
