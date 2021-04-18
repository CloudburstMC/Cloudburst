package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public class BlockBehaviorObserver extends BlockBehaviorSolid {

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, CloudBlockRegistry.get().getBlock(BlockTypes.OBSERVER).withTrait(
                BlockTraits.FACING_DIRECTION,
                (player != null ? player.getHorizontalDirection() : Direction.NORTH).getOpposite()
        ));
    }




}
