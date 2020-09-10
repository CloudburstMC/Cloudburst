package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.block.BlockFadeEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;

public class BlockBehaviorOreRedstoneGlowing extends BlockBehaviorOreRedstone {

    @Override
    public int getLightLevel(Block block) {
        return 9;
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(BlockIds.REDSTONE_ORE);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_SCHEDULED || type == Level.BLOCK_UPDATE_RANDOM) {
            BlockFadeEvent event = new BlockFadeEvent(block, BlockState.get(BlockIds.REDSTONE_ORE));
            block.getLevel().getServer().getEventManager().fire(event);
            if (!event.isCancelled()) {
                block.set(event.getNewState(), false, false);
            }

            return Level.BLOCK_UPDATE_WEAK;
        }

        return 0;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}
