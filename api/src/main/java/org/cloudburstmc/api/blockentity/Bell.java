package org.cloudburstmc.api.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.block.BellRingEvent;
import org.cloudburstmc.api.util.Direction;

/**
 * A placed bell.
 */
public interface Bell extends BlockEntity {

    /**
     * Attempts to ring this bell, firing a {@link BellRingEvent} before it rings.
     *
     * @param entity    the entity ringing the bell, if any
     * @param direction the horizontal side struck, or null to use the bell's facing
     * @return whether the bell rang
     * @throws IllegalArgumentException if the direction is vertical
     */
    boolean ring(@Nullable Entity entity, @Nullable Direction direction);

    /**
     * Attempts to ring this bell in the direction it faces.
     *
     * @return whether the bell rang
     */
    default boolean ring() {
        return this.ring(null, null);
    }

    /**
     * Returns whether the bell is shaking after a ring.
     *
     * @return whether the bell is shaking
     */
    boolean isShaking();

    /**
     * Returns the time since the bell was rung, in ticks.
     *
     * @return the elapsed ticks, or zero when the bell is not shaking
     */
    int getShakingTicks();
}
