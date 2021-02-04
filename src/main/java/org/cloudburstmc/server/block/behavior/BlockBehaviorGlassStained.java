package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

public class BlockBehaviorGlassStained extends BlockBehaviorGlass {

    @Override
    public BlockColor getColor(Block block) {
        return getDyeColor(block).getColor();
    }

    public DyeColor getDyeColor(Block block) {
        return block.getState().ensureTrait(BlockTraits.COLOR);
    }
}
