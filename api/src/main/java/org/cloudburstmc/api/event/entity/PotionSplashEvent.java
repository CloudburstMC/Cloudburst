package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.projectile.SplashPotion;
import org.cloudburstmc.api.event.Cancellable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Called before a splash potion applies its effects to nearby entities.
 */
public class PotionSplashEvent extends EntityEvent implements Cancellable {
    private final Map<Living, Double> affectedEntities;

    /**
     * Creates a splash potion application event.
     *
     * @param potion           the thrown potion
     * @param affectedEntities entities and their initial effect intensities
     */
    public PotionSplashEvent(SplashPotion potion, Map<? extends Living, Double> affectedEntities) {
        this.entity = requireNonNull(potion, "potion");
        this.affectedEntities = new LinkedHashMap<>();
        requireNonNull(affectedEntities, "affectedEntities").forEach(this::setIntensity);
    }

    @Override
    public SplashPotion getEntity() {
        return (SplashPotion) this.entity;
    }

    /**
     * Returns the thrown potion.
     *
     * @return the thrown potion
     */
    public SplashPotion getPotion() {
        return this.getEntity();
    }

    /**
     * Returns a snapshot of entities affected by the potion.
     *
     * @return affected entities
     */
    public Collection<Living> getAffectedEntities() {
        return List.copyOf(this.affectedEntities.keySet());
    }

    /**
     * Returns the effect intensity for an entity.
     *
     * @param entity the entity to query
     * @return a value from {@code 0.0} to {@code 1.0}, or {@code 0.0} when unaffected
     */
    public double getIntensity(Living entity) {
        return this.affectedEntities.getOrDefault(requireNonNull(entity, "entity"), 0.0);
    }

    /**
     * Changes the effect intensity for an entity. A non-positive value removes the entity.
     * Values greater than {@code 1.0} are limited to {@code 1.0}.
     *
     * @param entity    the entity to update
     * @param intensity the new finite intensity
     */
    public void setIntensity(Living entity, double intensity) {
        requireNonNull(entity, "entity");
        checkArgument(Double.isFinite(intensity), "intensity must be finite");

        if (intensity <= 0) {
            this.affectedEntities.remove(entity);
        } else {
            this.affectedEntities.put(entity, Math.min(intensity, 1.0));
        }
    }
}
