package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.player.CloudPlayer;

public class BlockBehaviorPodzol extends BlockBehaviorDirt {


    @Override
    public boolean canBeActivated(Block block) {
        return false;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, CloudPlayer player) {
        return false;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.SPRUCE_BLOCK_COLOR;
    }
}
