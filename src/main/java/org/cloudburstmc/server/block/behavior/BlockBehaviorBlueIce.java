package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

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
