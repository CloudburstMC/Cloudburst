package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

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
    public boolean onBreak(Block block, ItemStack item, CloudPlayer player) {
        return this.onBreak(block, item);
    }


}
