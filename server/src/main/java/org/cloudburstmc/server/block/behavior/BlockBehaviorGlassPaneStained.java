package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DyeColor;

/**
 * Created by CreeperFace on 7.8.2017.
 */
public class BlockBehaviorGlassPaneStained extends BlockBehaviorGlassPane {

    public BlockBehaviorGlassPaneStained(Identifier id) {
        super(id);
    }

    @Override
    public BlockColor getColor() {
        return getDyeColor().getColor();
    }

    public DyeColor getDyeColor() {
        return DyeColor.getByWoolData(getMeta());
    }
}
