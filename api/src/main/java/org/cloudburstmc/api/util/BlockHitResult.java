package org.cloudburstmc.api.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.math.vector.Vector3f;

import static java.util.Objects.requireNonNull;

/**
 * An intersection with a block shape or liquid at a block position.
 * The face is absent when the trace starts inside the shape.
 *
 * @param position the exact intersection
 * @param block    the block at the hit position
 * @param face     the entered face, if known
 * @param liquid   whether the liquid shape was hit instead of the block shape
 */
public record BlockHitResult(Vector3f position, Block block, @Nullable Direction face, boolean liquid) implements HitResult {

    public BlockHitResult {
        requireNonNull(position, "position");
        requireNonNull(block, "block");
    }
}
