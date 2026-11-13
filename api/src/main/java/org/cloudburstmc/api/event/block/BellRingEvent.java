package org.cloudburstmc.api.event.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.util.Direction;

import java.util.Objects;

/**
 * Called before a bell rings. Cancelling prevents the sound and animation.
 */
public class BellRingEvent extends BlockEvent implements Cancellable {

    private final Direction direction;
    private final @Nullable Entity entity;

    /**
     * Creates an event for a bell strike.
     *
     * @param block     the bell
     * @param direction the horizontal side struck
     * @param entity    the entity ringing the bell, if any
     */
    public BellRingEvent(Block block, Direction direction, @Nullable Entity entity) {
        super(block);
        this.direction = Objects.requireNonNull(direction, "direction");
        this.entity = entity;
    }

    /**
     * Returns the horizontal side struck.
     *
     * @return the strike direction
     */
    public Direction getDirection() {
        return this.direction;
    }

    /**
     * Returns the entity that rang the bell, if any.
     *
     * @return the entity, or null for a ring without an entity
     */
    public @Nullable Entity getEntity() {
        return this.entity;
    }
}
