package org.cloudburstmc.api.util;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.math.vector.Vector3f;

import static java.util.Objects.requireNonNull;

/**
 * An intersection with an entity hitbox.
 */
public record EntityHitResult(Vector3f position, Entity entity) implements HitResult {

    public EntityHitResult {
        requireNonNull(position, "position");
        requireNonNull(entity, "entity");
    }
}
