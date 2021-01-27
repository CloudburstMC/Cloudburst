package org.cloudburstmc.api.enchantment;

import lombok.val;
import org.cloudburstmc.api.item.ItemStack;

public enum EnchantmentTarget {
    ALL,
    ARMOR,
    ARMOR_HEAD,
    ARMOR_TORSO,
    ARMOR_LEGS,
    ARMOR_FEET,
    SWORD,
    DIGGER,
    FISHING_ROD,
    BREAKABLE,
    BOW,
    WEARABLE,
    TRIDENT,
    CROSSBOW;

    public boolean canEnchantItem(ItemStack item) {
        if (this == ALL) {
            return true;
        }

        val behavior = item.getBehavior();

        if (this == BREAKABLE && behavior.getMaxDurability() >= 0) {
            return true;
        }

/*        if (behavior instanceof ItemArmorBehavior) { //TODO: Fix
            if (this == ARMOR) {
                return true;
            }

            switch (this) {
                case ARMOR_HEAD:
                    return behavior.isHelmet();
                case ARMOR_TORSO:
                    return behavior.isChestplate();
                case ARMOR_LEGS:
                    return behavior.isLeggings();
                case ARMOR_FEET:
                    return behavior.isBoots();
                default:
                    return false;
            }
        }*/

        val type = item.getType();

/*        switch (this) { //TODO: Move to Server repo, CloudEnchantmentTarget?
            case SWORD:
                return behavior.isSword();
            case DIGGER:
                return behavior.isPickaxe() || behavior.isShovel() || behavior.isAxe();
            case BOW:
                return type == ItemTypes.BOW;
            case FISHING_ROD:
                return type == ItemTypes.FISHING_ROD;
            case WEARABLE:
                return behavior.isArmor() || type == ItemTypes.ELYTRA || type == ItemTypes.SKULL || type == BlockTypes.PUMPKIN;
            case TRIDENT:
                return type == ItemTypes.TRIDENT;
            default:
                return false;
        }*/
        return false;
    }
}
