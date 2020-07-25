package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

public class BlockBehaviorGlassStained extends BlockBehaviorGlass {

    @Override
    public BlockColor getColor(BlockState state) {
        return DyeColor.getByWoolData(getMeta()).getColor();
    }

    public DyeColor getDyeColor() {
        return DyeColor.getByWoolData(getMeta());
    }

    @Override
    public final void setMeta(int meta) {
        this.meta = meta;
    }
}
