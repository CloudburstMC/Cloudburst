package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.TerracottaColor;
import org.cloudburstmc.server.utils.data.DyeColor;

public class BlockBehaviorTerracotta extends BlockBehaviorSolid {

    @Override
    public BlockColor getColor(Block block) {
        return TerracottaColor.getBlockColor(block.getState().getType());
    }

    public DyeColor getDyeColor(Block block) {
        return TerracottaColor.getDyeColor(block.getState().getType());
    }
}
