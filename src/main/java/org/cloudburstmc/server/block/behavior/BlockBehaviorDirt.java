package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DirtType;

import static org.cloudburstmc.api.block.BlockTypes.DIRT;
import static org.cloudburstmc.api.block.BlockTypes.FARMLAND;
import static org.cloudburstmc.server.block.BlockTraits.DIRT_TYPE;

public class BlockBehaviorDirt extends BlockBehaviorSolid {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }


    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        val behavior = item.getBehavior();
        if (behavior.isHoe()) {
            behavior.useOn(item, block);
            block.set(BlockState.get(block.getState().ensureTrait(DIRT_TYPE) == DirtType.NORMAL ? FARMLAND : DIRT), true);
            return true;
        }

        return false;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{ItemStack.get(DIRT)};
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.DIRT_BLOCK_COLOR;
    }
}
