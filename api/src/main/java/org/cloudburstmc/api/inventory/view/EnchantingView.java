package org.cloudburstmc.api.inventory.view;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;

import java.util.List;

/**
 * Represents the item slots of an enchanting table container.
 * Exposes the item-to-enchant slot, the reagent (lapis lazuli) slot,
 * the current enchantment offers (up to 3), and the enchantment seed.
 *
 * <h3>Offers and seed are read-only</h3>
 * <p>The client computes the three enchantment options locally from the seed using the
 * vanilla algorithm. The server cannot push custom offers to the client; any attempt to
 * override them would be silently ignored. Therefore {@link #getOffers()} and
 * {@link #getSeed()} are intentionally read-only. The server updates the seed internally
 * whenever the player enchants an item or places a new item in the table.</p>
 */
public interface EnchantingView extends SlotGroup {

    ItemStack getInput();

    void setInput(ItemStack item);

    ItemStack getReagent();

    void setReagent(ItemStack item);

    /**
     * Returns the current enchantment offers computed by the client for this session.
     * There are always exactly 3 offers (top, middle, bottom); an offer may be
     * {@code null} if that slot has no valid enchantment for the current item.
     *
     * <p>This list reflects what the server believes the client is showing, based on the
     * current seed. Offers are computed client-side, so this list may not exactly
     * match what the client renders if the client version differs from the server's
     * enchantment tables.</p>
     *
     * <p>The returned list is unmodifiable.</p>
     *
     * @return an unmodifiable list of 3 offers (elements may be {@code null})
     */
    List<@Nullable EnchantmentOffer> getOffers();

    /**
     * Returns the enchantment seed used by the client to calculate which enchantments
     * are offered. Each time a player enchants an item or puts a new item in the table
     * the seed changes.
     *
     * @return the current enchantment seed
     */
    int getSeed();
}
