package org.cloudburstmc.api.util;

import org.cloudburstmc.math.vector.Vector3f;

/**
 * The outcome of a level ray trace.
 */
public sealed interface HitResult permits BlockHitResult, EntityHitResult, MissHitResult {

    /**
     * Returns the intersection or the point reached by a miss.
     */
    Vector3f position();
}
