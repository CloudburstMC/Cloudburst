package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public class AnvilPlaceHandler extends DefaultBlockPlaceHandler {

    public AnvilPlaceHandler(CloudBlockRegistry registry) {
        super(registry);
    }

    @Override
    protected BlockState applyDirectionTraits(BlockState blockState, Player player, Vector3i pos, Direction face) {
        Direction anvilFacing = player.getHorizontalDirection().rotateCounterClockwise();
        return blockState.withTrait(BlockTraits.CARDINAL_DIRECTION, anvilFacing.getCardinalDirection());
    }
}
