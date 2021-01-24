package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.block.BlockFadeEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.player.GameMode;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorIce extends BlockBehaviorTransparent {

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public float getFrictionFactor() {
        return 0.98f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean onBreak(Block block, Item item, Player player) {
        val level = block.getWorld();
        if (player.getGamemode() == GameMode.CREATIVE) {
            return removeBlock(block);
        }

        if (block.down().getState().inCategory(BlockCategory.SOLID)) {
            block.set(BlockState.get(BlockIds.WATER));
        } else {
            return removeBlock(block);
        }

        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_RANDOM) {
            if (block.getWorld().getBlockLightAt(block.getX(), block.getY(), block.getZ()) >= 12) {
                BlockFadeEvent event = new BlockFadeEvent(block, BlockState.get(BlockIds.WATER));
                block.getWorld().getServer().getEventManager().fire(event);
                if (!event.isCancelled()) {
                    block.getWorld().setBlock(block.getPosition(), event.getNewState(), true);
                }
                return World.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        return new Item[0];
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.ICE_BLOCK_COLOR;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}
