package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Creature;

import java.util.Objects;

/**
 * Fired before a creature is added to a level.
 */
public class CreatureSpawnEvent extends EntitySpawnEvent {

    private final CreatureSpawnReason reason;

    /**
     * Creates a creature spawn event.
     *
     * @param entity the creature being spawned
     * @param reason the reason for the spawn
     */
    public CreatureSpawnEvent(Creature entity, CreatureSpawnReason reason) {
        super(entity);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    /**
     * Returns why the creature is spawning.
     *
     * @return the spawn reason
     */
    public CreatureSpawnReason getReason() {
        return this.reason;
    }

    /**
     * Returns the creature being spawned.
     *
     * @return the creature
     */
    @Override
    public Creature getEntity() {
        return (Creature) this.entity;
    }
}
