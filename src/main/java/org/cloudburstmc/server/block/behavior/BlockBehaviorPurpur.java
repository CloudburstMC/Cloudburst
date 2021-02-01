package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.Direction.Axis;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorPurpur extends BlockBehaviorSolid {


    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, item.getBehavior().getBlock(item).withTrait(
                BlockTraits.AXIS,
                player != null ? player.getDirection().getAxis() : Axis.Y
        ));
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
        return ItemStack.get(block.getState().resetTrait(BlockTraits.AXIS));
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.MAGENTA_BLOCK_COLOR;
    }
}
