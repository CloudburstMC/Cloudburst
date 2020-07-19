package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DyeColor;

/**
 * Created by CreeperFace on 7.8.2017.
 */
public class BlockBehaviorGlassStained extends BlockBehaviorGlass {

    public BlockBehaviorGlassStained(Identifier id) {
        super(id);
    }

    @Override
    public BlockColor getColor() {
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
