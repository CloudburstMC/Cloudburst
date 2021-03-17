package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.item.ItemTypes;

public class BlockBehaviorDoorIron extends BlockBehaviorDoor {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (checkTool(block.getState(), hand)) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.IRON_DOOR);
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.IRON_BLOCK_COLOR;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        return false;
    }


}
