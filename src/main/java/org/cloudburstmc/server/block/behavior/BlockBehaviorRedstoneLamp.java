package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.redstone.RedstoneUpdateEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.world.World;
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
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val level = block.getWorld();
        if (level.isBlockPowered(block.getPosition())) {
            block.set(BlockState.get(BlockIds.LIT_REDSTONE_LAMP));
        } else {
            block.set(BlockState.get(BlockIds.REDSTONE_LAMP));
        }
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL || type == World.BLOCK_UPDATE_REDSTONE) {
            // Redstone event
            RedstoneUpdateEvent ev = new RedstoneUpdateEvent(block);
            block.getWorld().getServer().getEventManager().fire(ev);
            if (ev.isCancelled()) {
                return 0;
            }

            boolean powered = block.getWorld().isBlockPowered(block.getPosition());
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
    public Item[] getDrops(Block block, Item hand) {
        return new Item[]{
                Item.get(BlockIds.REDSTONE_LAMP)
        };
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }
}
