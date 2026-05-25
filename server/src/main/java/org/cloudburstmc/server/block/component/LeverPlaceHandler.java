package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.LeverDirection;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public class LeverPlaceHandler implements PlaceBlockHandler {

    private final CloudBlockRegistry registry;

    public LeverPlaceHandler(CloudBlockRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i pos, Direction face, Vector3f clickPos) {
        if (!isSupportingSolid(player.getLevel(), pos, face)) {
            return false;
        }

        Direction horizontal = player.getHorizontalDirection();
        LeverDirection leverDirection = LeverDirection.forDirection(face, horizontal);
        blockState = blockState.withTrait(BlockTraits.LEVER_DIRECTION, leverDirection);
        return player.getLevel().setBlockState(pos, blockState, true, true);
    }

    private boolean isSupportingSolid(Level level, Vector3i pos, Direction face) {
        Vector3i supportPos = face.getOpposite().relative(pos);
        BlockState support = level.getBlockState(supportPos.getX(), supportPos.getY(), supportPos.getZ());
        if (support == BlockStates.AIR) {
            return false;
        }
        return registry.getComponents(support.getType()).get(BlockComponents.SOLID).get();
    }
}
