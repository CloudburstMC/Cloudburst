package org.cloudburstmc.api.event.enchantment;

import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.inventory.InventoryEvent;
import org.cloudburstmc.api.inventory.EnchantingScreen;
import org.cloudburstmc.api.inventory.view.EnchantmentOffer;
import org.cloudburstmc.api.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fired before the selected enchantments are applied or any resources are spent.
 */
public class EnchantItemEvent extends InventoryEvent implements Cancellable {

    private final ItemStack item;
    private final int button;
    private final Map<EnchantmentType, Integer> enchantments = new LinkedHashMap<>();
    private int requiredLevel;

    /**
     * @param screen the enchanting table screen
     * @param item   the item about to be enchanted
     * @param button the selected offer index, from zero to two
     * @param offer  the selected offer
     */
    public EnchantItemEvent(EnchantingScreen screen, ItemStack item, int button, EnchantmentOffer offer) {
        super(screen);
        this.item = item;
        this.button = button;
        this.requiredLevel = offer.requiredLevel();
        offer.enchantments().forEach(enchantment -> this.enchantments.put(enchantment.type(), enchantment.level()));
    }

    @Override
    public EnchantingScreen getScreen() {
        return (EnchantingScreen) super.getScreen();
    }

    /**
     * @return the item about to be enchanted
     */
    public ItemStack getItem() {
        return this.item;
    }

    /**
     * @return the selected offer index, from zero to two
     */
    public int whichButton() {
        return this.button;
    }

    /**
     * @return the mutable map of enchantments to apply
     */
    public Map<EnchantmentType, Integer> getEnchantsToAdd() {
        return this.enchantments;
    }

    /**
     * @return the minimum player level required, not the levels spent
     */
    public int getRequiredLevel() {
        return this.requiredLevel;
    }

    /**
     * @param level the non-negative minimum player level required
     */
    public void setRequiredLevel(int level) {
        if (level < 0) {
            throw new IllegalArgumentException("Required level cannot be negative");
        }

        this.requiredLevel = level;
    }
}
