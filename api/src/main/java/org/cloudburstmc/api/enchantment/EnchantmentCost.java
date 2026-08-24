package org.cloudburstmc.api.enchantment;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Calculates an enchantment's modified cost for a level.
 *
 * @param base               cost at level one
 * @param perLevelAboveFirst amount added for each level above one
 */
public record EnchantmentCost(int base, int perLevelAboveFirst) {

    public EnchantmentCost {
        checkArgument(base >= 0, "base cannot be negative");
        checkArgument(perLevelAboveFirst >= 0, "perLevelAboveFirst cannot be negative");
    }

    /**
     * Creates a cost that is the same for every level.
     *
     * @param base constant cost
     * @return constant enchantment cost
     */
    public static EnchantmentCost constant(int base) {
        return new EnchantmentCost(base, 0);
    }

    /**
     * Creates a cost that increases after level one.
     *
     * @param base               cost at level one
     * @param perLevelAboveFirst amount added for each level above one
     * @return dynamic enchantment cost
     */
    public static EnchantmentCost dynamic(int base, int perLevelAboveFirst) {
        return new EnchantmentCost(base, perLevelAboveFirst);
    }

    /**
     * Calculates the cost for an enchantment level.
     *
     * @param level enchantment level, starting at one
     * @return calculated cost
     */
    public int calculate(int level) {
        checkArgument(level > 0, "level must be positive");
        return this.base + (level - 1) * this.perLevelAboveFirst;
    }
}
