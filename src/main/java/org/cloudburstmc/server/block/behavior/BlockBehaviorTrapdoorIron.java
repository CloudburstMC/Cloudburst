package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorTrapdoorIron extends BlockBehaviorTrapdoor {

    public BlockBehaviorTrapdoorIron() {
        this.blockColor = BlockColor.IRON_BLOCK_COLOR;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        return false;
    }

}
