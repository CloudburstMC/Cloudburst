package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;

import static java.util.Objects.requireNonNull;

/**
 * Fired when a non-player entity is removed from its level. This event is for
 * observation only and cannot be canceled.
 */
public class EntityRemoveEvent extends EntityEvent {

    private final EntityRemoveCause cause;

    /**
     * @param entity the entity being removed
     * @param cause  the reason for removal
     */
    public EntityRemoveEvent(Entity entity, EntityRemoveCause cause) {
        this.entity = requireNonNull(entity, "entity");
        this.cause = requireNonNull(cause, "cause");
    }

    /**
     * Returns why the entity was removed.
     */
    public EntityRemoveCause getCause() {
        return this.cause;
    }
}
