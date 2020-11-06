package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;

public class BlockBehaviorBlueIce extends BlockBehaviorIce {

    @Override
    public int onUpdate(Block block, int type) {
        return 0;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        block.set(BlockStates.AIR);
        return true;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item, Player player) {
        return this.onBreak(block, item);
    }


}
