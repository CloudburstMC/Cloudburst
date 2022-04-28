package org.cloudburstmc.server.item.behavior;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.behavior.DamageChanceBehavior;
import org.cloudburstmc.api.item.behavior.FloatItemBehavior;
import org.cloudburstmc.api.item.behavior.DestroySpeedBehavior;
import org.cloudburstmc.server.item.ItemUtils;

@UtilityClass
public class DefaultItemBehaviors {

    public static final DestroySpeedBehavior GET_DESTROY_SPEED = (behavior, itemStack, block) -> 1.0F;

    public static final FloatItemBehavior GET_DESTROY_SPEED_BONUS = (behavior, itemStack) -> {
        if (ItemUtils.isNull(itemStack)) {
            return 1.0F;
        }
        Enchantment enchantment = itemStack.get(ItemKeys.ENCHANTMENTS).get(EnchantmentTypes.EFFICIENCY);
        if (enchantment != null && enchantment.level() > 0) {
            return (enchantment.level() * enchantment.level()) + 1.0F;
        }
        return 0F;
    };

    public static final Boolean CAN_DESTROY_IN_CREATIVE = true;

    public static final DamageChanceBehavior GET_DAMAGE_CHANCE = (executor, unbreaking) -> 100 / (unbreaking + 1);
}
