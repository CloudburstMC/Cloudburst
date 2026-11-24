package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.LivingEntity;

import java.util.Objects;

/**
 * Fired before a living entity other than a player is added to a level.
 */
public class CreatureSpawnEvent extends EntitySpawnEvent {

    private final CreatureSpawnReason spawnReason;

    /**
     * @param entity      the living entity being spawned
     * @param spawnReason the reason for the spawn
     */
    public CreatureSpawnEvent(LivingEntity entity, CreatureSpawnReason spawnReason) {
        super(entity);
        this.spawnReason = Objects.requireNonNull(spawnReason, "spawnReason");
    }

    /**
     * Returns why the entity is spawning.
     */
    public CreatureSpawnReason getSpawnReason() {
        return this.spawnReason;
    }

    /**
     * Returns the living entity being spawned.
     */
    @Override
    public LivingEntity getEntity() {
        return (LivingEntity) this.entity;
    }
}
