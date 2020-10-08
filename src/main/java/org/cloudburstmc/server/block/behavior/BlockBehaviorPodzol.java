package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

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
