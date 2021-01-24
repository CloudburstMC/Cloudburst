package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.block.BlockFadeEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.world.World;

public class BlockBehaviorOreRedstoneGlowing extends BlockBehaviorOreRedstone {

    @Override
    public int getLightLevel(Block block) {
        return 9;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(BlockIds.REDSTONE_ORE);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_SCHEDULED || type == World.BLOCK_UPDATE_RANDOM) {
            BlockFadeEvent event = new BlockFadeEvent(block, BlockState.get(BlockIds.REDSTONE_ORE));
            block.getWorld().getServer().getEventManager().fire(event);
            if (!event.isCancelled()) {
                block.set(event.getNewState(), false, false);
            }

            return World.BLOCK_UPDATE_WEAK;
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
