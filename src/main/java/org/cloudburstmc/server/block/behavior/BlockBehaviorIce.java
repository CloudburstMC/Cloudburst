package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockCategory;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

public class BlockBehaviorIce extends BlockBehaviorTransparent {


    @Override
    public boolean onBreak(Block block, ItemStack item, CloudPlayer player) {
        val level = block.getLevel();
        if (player.getGamemode() == GameMode.CREATIVE) {
            return removeBlock(block);
        }

        if (block.down().getState().inCategory(BlockCategory.SOLID)) {
            block.set(BlockState.get(BlockTypes.WATER));
        } else {
            return removeBlock(block);
        }

        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_RANDOM) {
            if (block.getLevel().getBlockLightAt(block.getX(), block.getY(), block.getZ()) >= 12) {
                BlockFadeEvent event = new BlockFadeEvent(block, BlockState.get(BlockTypes.WATER));
                block.getLevel().getServer().getEventManager().fire(event);
                if (!event.isCancelled()) {
                    block.getLevel().setBlock(block.getPosition(), event.getNewState(), true);
                }
                return CloudLevel.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[0];
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.ICE_BLOCK_COLOR;
    }


}
