package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Fired when a brewing cycle completes in a {@link BrewingStand}. Captures the ingredient,
 * the three bottle slots, and the remaining fuel level at the moment of completion.
 * Cancelling this event prevents the brewed potions from being produced.
 */
public final class BrewFinishEvent extends Event implements Cancellable {

    private final BrewingStand brewingStand;
    private final ItemStack ingredient;
    private final ItemStack[] potions;
    private final int fuel;

    public BrewFinishEvent(BrewingStand blockEntity) {
        this.brewingStand = blockEntity;
        this.fuel = blockEntity.getFuelLevel();

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

    public int getFuel() {
        return fuel;
    }
}
