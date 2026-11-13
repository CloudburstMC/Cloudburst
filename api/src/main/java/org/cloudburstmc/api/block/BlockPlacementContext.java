package org.cloudburstmc.api.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;

/**
 * The original interaction used to choose where a block will be placed.
 *
 * @param clickedBlock  the block the player clicked
 * @param clickedFace   the face that was clicked
 * @param clickPosition the hit position relative to the clicked block
 * @param player        the player placing the block, or {@code null} if there is no player
 */
public record BlockPlacementContext(Block clickedBlock, Direction clickedFace, Vector3f clickPosition, @Nullable Player player) {
}
