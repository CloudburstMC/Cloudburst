package org.cloudburstmc.api.event.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Breedable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import static java.util.Objects.requireNonNull;

/**
 * Fired before breeding spawns an offspring. Cancelling prevents the birth and
 * clears the parents' love without refunding the food already fed to them.
 */
public class EntityBreedEvent extends EntityEvent implements Cancellable {

    private final Breedable firstParent;
    private final Breedable secondParent;
    private final @Nullable Player breeder;
    private final ItemStack bredWith;
    private int experience;

    /**
     * Creates a breeding event.
     *
     * @param offspring    the unspawned offspring
     * @param firstParent  the parent that initiated breeding
     * @param secondParent the other parent
     * @param breeder      the player who fed a parent, or {@code null} if unavailable
     * @param bredWith     the stack used to feed the first parent
     * @param experience   the non-negative experience reward
     * @throws IllegalArgumentException if the experience is negative
     */
    public EntityBreedEvent(Breedable offspring, Breedable firstParent, Breedable secondParent, @Nullable Player breeder, ItemStack bredWith, int experience) {
        this.entity = requireNonNull(offspring, "offspring");
        this.firstParent = requireNonNull(firstParent, "firstParent");
        this.secondParent = requireNonNull(secondParent, "secondParent");
        this.breeder = breeder;
        this.bredWith = requireNonNull(bredWith, "bredWith");
        this.setExperience(experience);
    }

    /**
     * Returns the unspawned offspring. The server handles spawning it.
     */
    @Override
    public Breedable getEntity() {
        return (Breedable) this.entity;
    }

    /**
     * Returns the parent that initiated breeding.
     */
    public Breedable getFirstParent() {
        return this.firstParent;
    }

    /**
     * Returns the other parent.
     */
    public Breedable getSecondParent() {
        return this.secondParent;
    }

    /**
     * Returns an available player who fed a parent, or null.
     */
    public @Nullable Player getBreeder() {
        return this.breeder;
    }

    /**
     * Returns the held stack used to feed the first parent.
     */
    public ItemStack getBredWith() {
        return this.bredWith;
    }

    /**
     * Returns the experience reward for a successful birth.
     */
    public int getExperience() {
        return this.experience;
    }

    /**
     * Sets the experience reward. Zero suppresses it.
     *
     * @throws IllegalArgumentException if the experience is negative
     */
    public void setExperience(int experience) {
        if (experience < 0) {
            throw new IllegalArgumentException("Breeding experience must not be negative");
        }

        this.experience = experience;
    }
}
