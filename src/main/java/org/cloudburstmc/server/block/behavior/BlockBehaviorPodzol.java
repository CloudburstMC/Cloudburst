package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;

public class BlockBehaviorPodzol extends BlockBehaviorDirt {


    @Override
    public boolean canBeActivated(Block block) {
        return false;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        return false;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.SPRUCE_BLOCK_COLOR;
    }
}
