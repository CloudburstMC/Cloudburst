package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.event.block.BlockFadeEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;

public class BlockBehaviorOreRedstoneGlowing extends BlockBehaviorOreRedstone {


    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(BlockTypes.REDSTONE_ORE);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_SCHEDULED || type == Level.BLOCK_UPDATE_RANDOM) {
            BlockFadeEvent event = new BlockFadeEvent(block, BlockState.get(BlockTypes.REDSTONE_ORE));
            block.getLevel().getServer().getEventManager().fire(event);
            if (!event.isCancelled()) {
                block.set(event.getNewState(), false, false);
            }

            return Level.BLOCK_UPDATE_WEAK;
        }

        return 0;
    }


}
