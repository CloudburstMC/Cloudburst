package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.behavior.Behavior;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

public interface PlaceBlockBehavior {

    boolean execute(Behavior<PlaceBlockBehavior.Executor> behavior, BlockState blockState, Player player,
                    Vector3i blockPosition, Direction face, Vector3f clickPosition);

    @FunctionalInterface
    interface Executor {

        boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face,
                        Vector3f clickPosition);
    }
}
