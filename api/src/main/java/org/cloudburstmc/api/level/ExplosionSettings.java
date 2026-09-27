package org.cloudburstmc.api.level;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;

import java.util.Objects;

/**
 * Parameters for a server-authoritative explosion.
 *
 * @param radius           explosion radius in blocks
 * @param blockInteraction how affected blocks are handled
 * @param causesFire       whether fire may be placed on exposed surfaces
 * @param sourceEntity     the entity directly responsible, or {@code null}
 * @param sourceBlock      the block that caused the explosion, or {@code null}
 */
public record ExplosionSettings(float radius, ExplosionBlockInteraction blockInteraction, boolean causesFire, @Nullable Entity sourceEntity, @Nullable Block sourceBlock) {

    public ExplosionSettings {
        if (!Float.isFinite(radius) || radius < 0) {
            throw new IllegalArgumentException("Explosion radius must be finite and non-negative");
        }

        Objects.requireNonNull(blockInteraction, "blockInteraction");
        if (sourceEntity != null && sourceBlock != null) {
            throw new IllegalArgumentException("An explosion cannot have both an entity and block source");
        }
    }
}
