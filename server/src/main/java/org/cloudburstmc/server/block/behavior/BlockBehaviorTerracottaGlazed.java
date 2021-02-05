package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.CloudPlayer;

public class BlockBehaviorTerracottaGlazed extends BlockBehaviorSolid {

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, CloudPlayer player) {
        return placeBlock(block, item.getBehavior().getBlock(item)
                .withTrait(
                        BlockTraits.FACING_DIRECTION,
                        player != null ? player.getHorizontalDirection().getOpposite() : Direction.NORTH
                ));
    }


}
