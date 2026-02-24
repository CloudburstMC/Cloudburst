package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.enchantment.EnchantmentType;

import java.util.Objects;

/**
 * Represents a single enchantment option presented to a player at an enchanting table.
 * <p>Instances are immutable value objects. Use {@link #of(EnchantmentType, int, int)} to create one.</p>
 */
public final class EnchantmentOffer {

    private final EnchantmentType enchantment;
    private final int enchantmentLevel;
    private final int cost;

    private EnchantmentOffer(EnchantmentType enchantment, int enchantmentLevel, int cost) {
        this.enchantment = Objects.requireNonNull(enchantment, "enchantment");
        if (enchantmentLevel < 1) throw new IllegalArgumentException("enchantmentLevel must be >= 1");
        if (cost < 1) throw new IllegalArgumentException("cost must be >= 1");
        this.enchantmentLevel = enchantmentLevel;
        this.cost = cost;
    }

    /**
     * Creates a new {@link EnchantmentOffer}.
     *
     * @param enchantment      the enchantment to apply
     * @param enchantmentLevel the enchantment level (≥ 1)
     * @param cost             the experience-level cost (≥ 1)
     * @return a new offer
     */
    public static EnchantmentOffer of(EnchantmentType enchantment, int enchantmentLevel, int cost) {
        return new EnchantmentOffer(enchantment, enchantmentLevel, cost);
    }

    /**
     * Returns the enchantment that will be applied when this offer is accepted.
     *
     * @return the enchantment type
     */
    public EnchantmentType enchantment() {
        return enchantment;
    }

    /**
     * Returns the level of the enchantment.
     *
     * @return the enchantment level
     */
    public int enchantmentLevel() {
        return enchantmentLevel;
    }

    /**
     * Returns the number of experience levels required to accept this offer.
     * This also equals the number of lapis lazuli consumed.
     *
     * @return the level cost
     */
    public int cost() {
        return cost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnchantmentOffer that)) return false;
        return enchantmentLevel == that.enchantmentLevel
                && cost == that.cost
                && Objects.equals(enchantment, that.enchantment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enchantment, enchantmentLevel, cost);
    }

    @Override
    public String toString() {
        return "EnchantmentOffer{enchantment=" + enchantment
                + ", level=" + enchantmentLevel
                + ", cost=" + cost + '}';
    }
}
