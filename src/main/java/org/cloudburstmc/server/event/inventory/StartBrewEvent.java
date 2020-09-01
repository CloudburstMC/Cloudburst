package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.server.blockentity.BrewingStand;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.behavior.Item;

/**
 * @author CreeperFace
 */
public class StartBrewEvent extends InventoryEvent implements Cancellable {

    private final BrewingStand brewingStand;
    private final Item ingredient;
    private final Item[] potions;

    public StartBrewEvent(BrewingStand blockEntity) {
        super(blockEntity.getInventory());
        this.brewingStand = blockEntity;

        this.ingredient = blockEntity.getInventory().getIngredient();

        this.potions = new Item[3];
        for (int i = 0; i < 3; i++) {
            this.potions[i] = blockEntity.getInventory().getItem(i);
        }
    }

    public BrewingStand getBrewingStand() {
        return brewingStand;
    }

    public Item getIngredient() {
        return ingredient;
    }

    public Item[] getPotions() {
        return potions;
    }

    /**
     * @param index Potion index in range 0 - 2
     * @return potion
     */
    public Item getPotion(int index) {
        return this.potions[index];
    }
}
