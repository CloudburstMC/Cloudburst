package org.cloudburstmc.api.entity;

import org.cloudburstmc.api.item.ItemStack;

/**
 * An ageable animal that can breed.
 */
public interface Breedable extends Ageable {

    /**
     * Returns whether this animal is an adult with no breeding cooldown.
     */
    boolean canBreed();

    /**
     * Returns whether this animal is looking for a mate.
     */
    boolean isInLove();

    /**
     * Returns whether this item can be fed to this animal for breeding or growth.
     */
    boolean isBreedingItem(ItemStack item);
}
