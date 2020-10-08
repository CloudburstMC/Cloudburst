package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

public class BlockBehaviorObserver extends BlockBehaviorSolid {

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, BlockState.get(BlockTypes.OBSERVER).withTrait(
                BlockTraits.FACING_DIRECTION,
                (player != null ? player.getHorizontalDirection() : Direction.NORTH).getOpposite()
        ));
    }




}
