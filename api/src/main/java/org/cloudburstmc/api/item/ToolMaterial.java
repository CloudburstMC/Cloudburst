package org.cloudburstmc.api.item;

import org.cloudburstmc.api.block.BlockTagKey;

/**
 * Describes the shared mining, durability, attack, and enchantability values for a tool material.
 */
public interface ToolMaterial {

    /**
     * Gets the block tag containing blocks this material cannot correctly harvest.
     *
     * @return the incorrect-for-drops block tag
     */
    BlockTagKey getIncorrectBlocksForDrops();

    /**
     * Gets the maximum durability value for tools made from this material.
     *
     * @return the tool durability
     */
    int getDurability();

    /**
     * Gets the mining speed used by tools made from this material.
     *
     * @return the mining speed
     */
    float getSpeed();

    /**
     * Gets the material attack damage bonus applied by tools made from this material.
     *
     * @return the attack damage bonus
     */
    float getAttackDamageBonus();

    /**
     * Gets the enchantability value used by tools made from this material.
     *
     * @return the tool enchantability
     */
    int getEnchantmentValue();
}
