package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.TerracottaColor;
import org.cloudburstmc.server.utils.data.DyeColor;

public class BlockBehaviorTerracotta extends BlockBehaviorSolid {

    @Override
    public BlockColor getColor(Block block) {
        return TerracottaColor.getBlockColor(block.getState().getId()); //TODO: replace with color?
    }

    public DyeColor getDyeColor(Block block) {
        return TerracottaColor.getDyeColor(block.getState().getId());
    }
}
