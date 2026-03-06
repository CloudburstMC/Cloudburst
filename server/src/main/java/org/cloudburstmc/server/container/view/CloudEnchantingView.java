package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.EnchantingView;
import org.cloudburstmc.api.inventory.view.EnchantmentOffer;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * View-layer slot group for the enchanting table. Holds two item slots: input (item to enchant)
 * and reagent (lapis lazuli). Tracks the three enchantment offers and the enchantment seed.
 * <p>
 * There is no separate output slot; the item is enchanted in-place in the input slot.
 * This is an ephemeral section with no block entity backing.
 */
public class CloudEnchantingView extends CloudSlotGroupBase implements EnchantingView {

    private static final int OFFER_COUNT = 3;

    private final List<EnchantmentOffer> offers;
    private int seed;

    public CloudEnchantingView() {
        super(SlotGroupTypes.ENCHANTING, new CloudContainer(2));
        this.offers = new ArrayList<>(Arrays.asList(null, null, null));
        this.seed = ThreadLocalRandom.current().nextInt();
    }

    @Override
    public ItemStack getInput() {
        return getItem(0);
    }

    @Override
    public void setInput(ItemStack item) {
        setItem(0, item);
    }

    @Override
    public ItemStack getReagent() {
        return getItem(1);
    }

    @Override
    public void setReagent(ItemStack item) {
        setItem(1, item);
    }

    @Override
    public List<EnchantmentOffer> getOffers() {
        return Collections.unmodifiableList(offers);
    }

    void updateOffers(EnchantmentOffer top, EnchantmentOffer middle, EnchantmentOffer bottom) {
        this.offers.set(0, top);
        this.offers.set(1, middle);
        this.offers.set(2, bottom);
    }

    @Override
    public int getSeed() {
        return seed;
    }

    void updateSeed(int seed) {
        this.seed = seed;
    }
}
