package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * @author Erik Miller | EinBexiii | Bex
 */
public class BlockBehaviorStairsPrismarineBricks extends BlockBehaviorStairsPrismarine {

    public BlockBehaviorStairsPrismarineBricks(Identifier id) {
        super(id);
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.DIAMOND_BLOCK_COLOR;
    }
}
