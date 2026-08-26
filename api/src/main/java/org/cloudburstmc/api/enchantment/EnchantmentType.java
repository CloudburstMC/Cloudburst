package org.cloudburstmc.api.enchantment;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.util.Identifier;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable definition for an enchantment type.
 *
 * @param id                  the numeric runtime ID used by the network protocol
 * @param identifier          the namespaced enchantment identifier
 * @param maxLevel            the highest supported enchantment level
 * @param rarity              the enchantment rarity used for weighted selection
 * @param treasure            whether this enchantment is only obtainable from treasure sources
 * @param cursed              whether this enchantment is a curse
 * @param target              the items this enchantment may apply to
 * @param exclusiveGroup      the group this enchantment belongs to, or {@code null} if it has no group
 * @param exclusiveWithGroups the groups this enchantment cannot be combined with
 * @param minCost             the minimum modified enchantment cost calculation
 * @param maxCost             the maximum modified enchantment cost calculation
 * @param anvilCost           the cost multiplier used when applying this enchantment in an anvil
 */
public record EnchantmentType(
        short id,
        Identifier identifier,
        int maxLevel,
        EnchantmentRarity rarity,
        boolean treasure,
        boolean cursed,
        EnchantmentTarget target,
        @Nullable EnchantmentExclusiveGroup exclusiveGroup,
        Set<EnchantmentExclusiveGroup> exclusiveWithGroups,
        EnchantmentCost minCost,
        EnchantmentCost maxCost,
        int anvilCost
) {
    public EnchantmentType {
        checkArgument(maxLevel > 0, "maxLevel must be positive");
        checkArgument(anvilCost >= 0, "anvilCost cannot be negative");
        checkNotNull(identifier, "identifier");
        checkNotNull(rarity, "rarity");
        checkNotNull(target, "target");
        exclusiveWithGroups = Set.copyOf(checkNotNull(exclusiveWithGroups, "exclusiveWithGroups"));
        checkNotNull(minCost, "minCost");
        checkNotNull(maxCost, "maxCost");
    }

    public EnchantmentType(short id, Identifier identifier, int maxLevel, EnchantmentRarity rarity,
                           boolean treasure, boolean cursed, EnchantmentTarget target,
                           EnchantmentCost minCost, EnchantmentCost maxCost, int anvilCost) {
        this(id, identifier, maxLevel, rarity, treasure, cursed, target, null, minCost, maxCost, anvilCost);
    }

    public EnchantmentType(short id, Identifier identifier, int maxLevel, EnchantmentRarity rarity,
                           boolean treasure, boolean cursed, EnchantmentTarget target,
                           @Nullable EnchantmentExclusiveGroup exclusiveGroup, EnchantmentCost minCost,
                           EnchantmentCost maxCost, int anvilCost) {
        this(id, identifier, maxLevel, rarity, treasure, cursed, target, exclusiveGroup,
                exclusiveGroup == null ? Set.of() : Set.of(exclusiveGroup), minCost, maxCost, anvilCost);
    }

    /**
     * Returns whether this enchantment cannot be combined with another enchantment type.
     *
     * @param other the other enchantment type
     * @return {@code true} if the enchantments conflict
     */
    public boolean conflictsWith(EnchantmentType other) {
        checkNotNull(other, "other");
        if (this.equals(other)) {
            return true;
        }

        EnchantmentExclusiveGroup otherGroup = other.exclusiveGroup();
        return (otherGroup != null && this.exclusiveWithGroups.contains(otherGroup))
                || (this.exclusiveGroup != null && other.exclusiveWithGroups().contains(this.exclusiveGroup));
    }

    /**
     * Gets the minimum modified enchantment cost for a level.
     *
     * @param level enchantment level, starting at one
     * @return minimum modified cost
     */
    public int minModifiedCost(int level) {
        checkArgument(level > 0, "level must be positive");
        checkArgument(level <= this.maxLevel, "level cannot be greater than maxLevel");
        return this.minCost.calculate(level);
    }

    /**
     * Gets the maximum modified enchantment cost for a level.
     *
     * @param level enchantment level, starting at one
     * @return maximum modified cost
     */
    public int maxModifiedCost(int level) {
        checkArgument(level > 0, "level must be positive");
        checkArgument(level <= this.maxLevel, "level cannot be greater than maxLevel");
        return this.maxCost.calculate(level);
    }
}
