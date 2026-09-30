package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Fired before a living entity's death is committed. Cancellation restores its
 * revival health and prevents death, item drops and experience drops.
 */
public class EntityDeathEvent extends EntityEvent implements Cancellable {

    private final DamageSource damageSource;
    private final List<ItemStack> drops;
    private int droppedExperience;
    private float reviveHealth;

    /**
     * @param entity            the dying entity
     * @param damageSource      the source responsible for death
     * @param drops             the proposed item drops, copied into an editable list
     * @param droppedExperience the non-negative experience to drop
     */
    public EntityDeathEvent(Living entity, DamageSource damageSource, List<ItemStack> drops, int droppedExperience) {
        this.entity = requireNonNull(entity, "entity");
        this.damageSource = requireNonNull(damageSource, "damageSource");
        this.drops = new ArrayList<>(List.copyOf(drops));
        this.setDroppedExperience(droppedExperience);
        this.reviveHealth = entity.getMaxHealth();
    }

    /**
     * @return the dying entity
     */
    @Override
    public Living getEntity() {
        return (Living) this.entity;
    }

    /**
     * @return the source responsible for death
     */
    public DamageSource getDamageSource() {
        return this.damageSource;
    }

    /**
     * Returns the editable drop list. Listeners may add, replace or remove drops.
     * Entries must be non-null. Changes do not modify the entity's inventory.
     *
     * @return the item drops
     */
    public List<ItemStack> getDrops() {
        return this.drops;
    }

    /**
     * @return the experience to drop, not the entity's level or retained experience
     */
    public int getDroppedExperience() {
        return this.droppedExperience;
    }

    /**
     * @param experience the non-negative experience to drop
     * @throws IllegalArgumentException if experience is negative
     */
    public void setDroppedExperience(int experience) {
        if (experience < 0) {
            throw new IllegalArgumentException("Dropped experience cannot be negative");
        }

        this.droppedExperience = experience;
    }

    /**
     * @return the health restored on cancellation, initially the entity's maximum health
     */
    public float getReviveHealth() {
        return this.reviveHealth;
    }

    /**
     * @param health the finite, positive health restored if death is canceled
     * @throws IllegalArgumentException if health is not in (0, maximum health]
     */
    public void setReviveHealth(float health) {
        if (!Float.isFinite(health) || health <= 0 || health > this.getEntity().getMaxHealth()) {
            throw new IllegalArgumentException("Revival health must be positive and no greater than maximum health");
        }

        this.reviveHealth = health;
    }
}
