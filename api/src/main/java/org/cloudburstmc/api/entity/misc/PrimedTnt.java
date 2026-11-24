package org.cloudburstmc.api.entity.misc;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Explosive;

/**
 * A lit TNT entity with a countdown to detonation.
 */
public interface PrimedTnt extends Explosive {

    /**
     * Returns the entity credited as the source, or {@code null} if it is unknown.
     */
    @Nullable
    default Entity getSource() {
        return getOwner();
    }

    /**
     * Changes the credited source, or clears it with {@code null}.
     */
    default void setSource(@Nullable Entity source) {
        this.setOwner(source);
    }

    /**
     * Returns the remaining fuse time in ticks.
     */
    @NonNegative
    int getFuseTicks();

    /**
     * Sets the remaining fuse time in ticks. Zero detonates on the next tick.
     *
     * @throws IllegalArgumentException if the fuse time is negative
     */
    void setFuseTicks(@NonNegative int fuseTicks);
}
