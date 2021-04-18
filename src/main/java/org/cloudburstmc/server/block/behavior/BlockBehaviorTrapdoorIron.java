package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.BlockColor;

public class BlockBehaviorTrapdoorIron extends BlockBehaviorTrapdoor {

    public BlockBehaviorTrapdoorIron() {
        this.blockColor = BlockColor.IRON_BLOCK_COLOR;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        return false;
    }

}
