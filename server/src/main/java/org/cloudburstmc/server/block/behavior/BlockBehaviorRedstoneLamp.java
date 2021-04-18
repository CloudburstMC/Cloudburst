package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.redstone.RedstoneUpdateEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class BlockBehaviorRedstoneLamp extends BlockBehaviorSolid {

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        var level = (CloudLevel) block.getLevel();
        var state = CloudBlockRegistry.get().getBlock(BlockTypes.REDSTONE_LAMP);
        if (level.isBlockPowered(block.getPosition())) {
            state = state.withTrait(BlockTraits.IS_POWERED, true);
        }

        block.set(state.withTrait(BlockTraits.IS_EXTINGUISHED, true));
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL || type == CloudLevel.BLOCK_UPDATE_REDSTONE) {
            // Redstone event
            RedstoneUpdateEvent ev = new RedstoneUpdateEvent(block);
            block.getLevel().getServer().getEventManager().fire(ev);
            if (ev.isCancelled()) {
                return 0;
            }

            boolean powered = ((CloudLevel) block.getLevel()).isBlockPowered(block.getPosition());
            var state = block.getState();

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
                CloudItemRegistry.get().getItem(BlockTypes.REDSTONE_LAMP)
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
