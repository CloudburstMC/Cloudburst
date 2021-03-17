package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.api.util.data.TerracottaColor;

public class BlockBehaviorTerracotta extends BlockBehaviorSolid {

    @Override
    public BlockColor getColor(Block block) {
        return TerracottaColor.getBlockColor(block.getState().getType().getId()); //TODO: replace with color?
    }

    public DyeColor getDyeColor(Block block) {
        return TerracottaColor.getDyeColor(block.getState().getType().getId());
    }
}
