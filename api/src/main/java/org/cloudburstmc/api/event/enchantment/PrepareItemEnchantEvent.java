package org.cloudburstmc.api.event.enchantment;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.inventory.InventoryEvent;
import org.cloudburstmc.api.inventory.EnchantingScreen;
import org.cloudburstmc.api.inventory.view.EnchantmentOffer;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * Fired when an enchanting table prepares offers for an item.
 */
public class PrepareItemEnchantEvent extends InventoryEvent implements Cancellable {

    private final ItemStack item;
    private final int bookshelfBonus;
    private final List<@Nullable EnchantmentOffer> offers;

    /**
     * @param screen         the enchanting table screen
     * @param item           the item being considered
     * @param bookshelfBonus the number of contributing bookshelves
     * @param offers         the three initial offers, with null for unavailable offers
     */
    public PrepareItemEnchantEvent(EnchantingScreen screen, ItemStack item, int bookshelfBonus, List<@Nullable EnchantmentOffer> offers) {
        super(screen);
        if (offers.size() != 3) {
            throw new IllegalArgumentException("Expected three offer slots");
        }
        this.item = item;
        this.bookshelfBonus = bookshelfBonus;
        this.offers = Arrays.asList(offers.toArray(EnchantmentOffer[]::new));
    }

    @Override
    public EnchantingScreen getScreen() {
        return (EnchantingScreen) super.getScreen();
    }

    /**
     * @return the item being considered
     */
    public ItemStack getItem() {
        return this.item;
    }

    /**
     * @return the number of bookshelves contributing to the offers, capped at fifteen
     */
    public int getBookshelfBonus() {
        return this.bookshelfBonus;
    }

    /**
     * @return the three replaceable offer slots in display order, with null for unavailable offers
     */
    public List<@Nullable EnchantmentOffer> getOffers() {
        return this.offers;
    }
}
