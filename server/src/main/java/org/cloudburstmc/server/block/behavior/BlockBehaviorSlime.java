package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by Pub4Game on 21.02.2016.
 */
public class BlockBehaviorSlime extends BlockBehaviorSolid {

    public BlockBehaviorSlime(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 0;
    }

    @Override
    public float getResistance() {
        return 0;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.GRASS_BLOCK_COLOR;
    }
}
