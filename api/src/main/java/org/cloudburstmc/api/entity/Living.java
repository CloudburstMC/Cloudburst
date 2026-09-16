package org.cloudburstmc.api.entity;

/**
 * An entity with health and other living-entity behavior.
 */
public interface Living extends Entity, ProjectileSource {

    /**
     * Performs this entity's standard attack against a target.
     *
     * @param target the entity to attack
     * @return whether the attack damaged the target
     */
    boolean attack(Entity target);
}
