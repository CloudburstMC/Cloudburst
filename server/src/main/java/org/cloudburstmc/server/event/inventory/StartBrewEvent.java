package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.server.blockentity.BrewingStand;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.ItemStack;

/**
 * @author CreeperFace
 */
public class StartBrewEvent extends InventoryEvent implements Cancellable {

    private final BrewingStand brewingStand;
    private final ItemStack ingredient;
    private final ItemStack[] potions;

    public StartBrewEvent(BrewingStand blockEntity) {
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
