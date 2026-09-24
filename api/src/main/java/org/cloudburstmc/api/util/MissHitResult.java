package org.cloudburstmc.api.util;

import org.cloudburstmc.math.vector.Vector3f;

import static java.util.Objects.requireNonNull;

/**
 * A trace that reached its end or stopped at unloaded terrain without a hit.
 *
 * @param position the farthest reached point
 * @param reason   why tracing stopped
 */
public record MissHitResult(Vector3f position, MissReason reason) implements HitResult {

    public MissHitResult {
        requireNonNull(position, "position");
        requireNonNull(reason, "reason");
    }
}
