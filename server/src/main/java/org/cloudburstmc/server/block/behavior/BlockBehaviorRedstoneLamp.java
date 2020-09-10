package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.redstone.RedstoneUpdateEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorRedstoneLamp extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 0.3f;
    }

    @Override
    public float getResistance() {
        return 1.5f;
    }

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val level = block.getLevel();
        if (level.isBlockPowered(block.getPosition())) {
            block.set(BlockState.get(BlockIds.LIT_REDSTONE_LAMP));
        } else {
            block.set(BlockState.get(BlockIds.REDSTONE_LAMP));
        }
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_REDSTONE) {
            // Redstone event
            RedstoneUpdateEvent ev = new RedstoneUpdateEvent(block);
            block.getLevel().getServer().getEventManager().fire(ev);
            if (ev.isCancelled()) {
                return 0;
            }

            boolean powered = block.getLevel().isBlockPowered(block.getPosition());
            val blockType = block.getState().getType();

            if (powered && blockType == BlockIds.REDSTONE_LAMP) {
                block.set(BlockState.get(BlockIds.LIT_REDSTONE_LAMP), false, false);
                return 1;
            }

            if (!powered && blockType == BlockIds.LIT_REDSTONE_LAMP) {
                block.set(BlockState.get(BlockIds.REDSTONE_LAMP), false, false);
                return 1;
            }
        }

        return 0;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                ItemStack.get(BlockIds.REDSTONE_LAMP)
        };
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }
}
