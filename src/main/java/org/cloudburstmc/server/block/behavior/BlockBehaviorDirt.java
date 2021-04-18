package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.api.util.data.DirtType;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.block.BlockTraits.DIRT_TYPE;
import static org.cloudburstmc.api.block.BlockTypes.DIRT;
import static org.cloudburstmc.api.block.BlockTypes.FARMLAND;

public class BlockBehaviorDirt extends BlockBehaviorSolid {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }


    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        var behavior = item.getBehavior();
        if (behavior.isHoe()) {
            behavior.useOn(item, block.getState());
            block.set(CloudBlockRegistry.get().getBlock(block.getState().ensureTrait(DIRT_TYPE) == DirtType.NORMAL ? FARMLAND : DIRT), true);
            return true;
        }

        return false;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{CloudItemRegistry.get().getItem(DIRT)};
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.DIRT_BLOCK_COLOR;
    }
}
