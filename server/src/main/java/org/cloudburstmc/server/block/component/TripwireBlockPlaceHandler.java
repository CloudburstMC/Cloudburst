package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public class TripwireBlockPlaceHandler implements PlaceBlockHandler {

    private final CloudBlockRegistry registry;

    public TripwireBlockPlaceHandler(CloudBlockRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i pos, Direction face, Vector3f clickPos) {
        if (player == null) {
            return false;
        }

        CloudLevel level = (CloudLevel) player.getLevel();
        Vector3i below = pos.sub(0, 1, 0);
        BlockState support = level.getBlockState(below.getX(), below.getY(), below.getZ());
        boolean suspended = !registry.getComponents(support.getType()).get(BlockComponents.SOLID).get();

        blockState = blockState.withTrait(BlockTraits.IS_SUSPENDED, suspended);
        if (!level.setBlockState(pos, blockState, true, true)) {
            return false;
        }

        TripwireCalculator.notifyHooksAround(level, pos);
        return true;
    }
}
