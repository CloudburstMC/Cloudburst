package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.CloudLevel;

public class BlockBehaviorOreRedstoneGlowing extends BlockBehaviorOreRedstone {


    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(BlockTypes.REDSTONE_ORE);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_SCHEDULED || type == CloudLevel.BLOCK_UPDATE_RANDOM) {
            BlockFadeEvent event = new BlockFadeEvent(block, BlockState.get(BlockTypes.REDSTONE_ORE));
            block.getLevel().getServer().getEventManager().fire(event);
            if (!event.isCancelled()) {
                block.set(event.getNewState(), false, false);
            }

            return CloudLevel.BLOCK_UPDATE_WEAK;
        }

        return 0;
    }


}
