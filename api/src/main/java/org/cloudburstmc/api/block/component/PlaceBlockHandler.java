package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

@FunctionalInterface
public interface PlaceBlockHandler {

    boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition);
}
