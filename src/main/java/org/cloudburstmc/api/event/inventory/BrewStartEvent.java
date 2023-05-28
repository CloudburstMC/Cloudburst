package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;

/**
 * @author CreeperFace
 */
public final class BrewStartEvent extends InventoryEvent implements Cancellable {

    private final BrewingStand brewingStand;
    private final ItemStack ingredient;
    private final ItemStack[] potions;

    public BrewStartEvent(BrewingStand blockEntity) {
        super(blockEntity.getInventory());
        this.brewingStand = blockEntity;

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
}
