package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.event.block.BlockFadeEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
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
        if (player.getGamemode() == GameMode.CREATIVE) {
            return this.getLevel().setBlock(this.getPosition(), BlockState.AIR, true);
        }

        if (down().isSolid()) {
            return this.getLevel().setBlock(this.getPosition(), BlockState.get(BlockTypes.WATER), true);
        } else {
            return this.getLevel().setBlock(this.getPosition(), BlockState.AIR, true);
        }
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (this.getLevel().getBlockLightAt(this.getX(), this.getY(), this.getZ()) >= 12) {
                BlockFadeEvent event = new BlockFadeEvent(this, BlockState.get(BlockTypes.WATER));
                level.getServer().getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    level.setBlock(this.getPosition(), event.getNewState(), true);
                }
                return Level.BLOCK_UPDATE_NORMAL;
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
