package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import lombok.var;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.event.redstone.RedstoneUpdateEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorRedstoneLamp extends BlockBehaviorSolid {

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val level = block.getLevel();
        var state = BlockState.get(BlockTypes.REDSTONE_LAMP);
        if (level.isBlockPowered(block.getPosition())) {
            state = state.withTrait(BlockTraits.IS_POWERED, true);
        }

        block.set(state.withTrait(BlockTraits.IS_EXTINGUISHED, true));
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
            val state = block.getState();

            if (state.ensureTrait(BlockTraits.IS_POWERED) != powered) {
                block.set(state.toggleTrait(BlockTraits.IS_POWERED), false, false);
                return 1;
            }
        }

        return 0;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                ItemStack.get(BlockTypes.REDSTONE_LAMP)
        };
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public int getLightLevel(Block block) {
        return block.getState().ensureTrait(BlockTraits.IS_POWERED) ? 15 : 0;
    }
}
