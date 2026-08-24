package org.cloudburstmc.api.enchantment;

/**
 * Named enchantment groups used by compatibility rules.
 */
public enum EnchantmentExclusiveGroup {
    /**
     * Mutually-exclusive armor protection enchantments.
     */
    ARMOR,
    /**
     * Boot movement enchantments such as Depth Strider and Frost Walker.
     */
    BOOTS,
    /**
     * Bow enchantments such as Infinity and Mending.
     */
    BOW,
    /**
     * Crossbow projectile modifiers such as Multishot and Piercing.
     */
    CROSSBOW_PROJECTILE,
    /**
     * Mutually-exclusive direct damage modifiers.
     */
    DAMAGE,
    /**
     * Mining result modifiers such as Silk Touch and Fortune.
     */
    MINING,
    /**
     * Trident enchantments that conflict with Riptide.
     */
    RIPTIDE_TRIDENT
}
