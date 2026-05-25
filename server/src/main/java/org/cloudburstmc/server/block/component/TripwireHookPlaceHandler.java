package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public class TripwireHookPlaceHandler implements PlaceBlockHandler {

    private final CloudBlockRegistry registry;

    public TripwireHookPlaceHandler(CloudBlockRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i pos, Direction face, Vector3f clickPos) {
        if (!face.getAxis().isHorizontal()) {
            return false;
        }

        Vector3i wallPos = face.getOpposite().relative(pos);
        BlockState wall = player.getLevel().getBlockState(wallPos.getX(), wallPos.getY(), wallPos.getZ());
        if (wall == BlockStates.AIR || !registry.getComponents(wall.getType()).get(BlockComponents.SOLID).get()) {
            return false;
        }

        blockState = blockState.withTrait(BlockTraits.DIRECTION, face);
        if (!player.getLevel().setBlockState(pos, blockState, true, true)) {
            return false;
        }

        TripwireCalculator.calculateState((CloudLevel) player.getLevel(), pos, blockState, false);
        return true;
    }
}
