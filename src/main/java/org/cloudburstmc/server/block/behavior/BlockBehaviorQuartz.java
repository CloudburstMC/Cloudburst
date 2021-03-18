package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Direction.Axis;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class BlockBehaviorQuartz extends BlockBehaviorSolid {


    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, item.getBehavior().getBlock(item).withTrait(
                BlockTraits.AXIS,
                player != null ? player.getDirection().getAxis() : Axis.Y
        ));
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(block.getState().withTrait(BlockTraits.AXIS, BlockTraits.AXIS.getDefaultValue()));
    }


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.QUARTZ_BLOCK_COLOR;
    }


}
