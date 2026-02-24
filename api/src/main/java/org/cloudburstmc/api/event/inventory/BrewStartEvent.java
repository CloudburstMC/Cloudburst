package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Fired when a new brewing cycle begins in a {@link BrewingStand}. Captures the ingredient
 * and the three bottle slots at the moment the cycle starts.
 * Cancelling this event prevents the cycle from beginning.
 */
public final class BrewStartEvent extends Event implements Cancellable {

    private final BrewingStand brewingStand;
    private final ItemStack ingredient;
    private final ItemStack[] potions;

    public BrewStartEvent(BrewingStand blockEntity) {
        this.brewingStand = blockEntity;

        this.ingredient = blockEntity.getIngredient();

        this.potions = new ItemStack[3];
        for (int i = 0; i < 3; i++) {
            this.potions[i] = blockEntity.getBottle(i);
        }
    }

    public BrewingStand getBrewingStand() {
        return brewingStand;
    }

    public ItemStack getIngredient() {
        return ingredient;
    }

    public ItemStack[] getPotions() {
        return potions;
    }

    /**
     * @param index Potion index in range 0 - 2
     * @return potion
     */
    public ItemStack getPotion(int index) {
        return this.potions[index];
    }
}
