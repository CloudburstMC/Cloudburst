package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.server.blockentity.BrewingStand;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.ItemStack;

/**
 * @author CreeperFace
 */
public class BrewEvent extends InventoryEvent implements Cancellable {

    private final BrewingStand brewingStand;
    private final ItemStack ingredient;
    private final ItemStack[] potions;
    private final int fuel;

    public BrewEvent(BrewingStand blockEntity) {
        super(blockEntity.getInventory());
        this.brewingStand = blockEntity;
        this.fuel = blockEntity.getFuelAmount();

        this.ingredient = blockEntity.getInventory().getIngredient();

        this.potions = new ItemStack[3];
        for (int i = 0; i < 3; i++) {
            this.potions[i] = blockEntity.getInventory().getItem(i);
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
