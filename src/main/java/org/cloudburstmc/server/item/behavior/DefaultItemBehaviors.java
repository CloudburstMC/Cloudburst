package org.cloudburstmc.server.item.behavior;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.api.block.BlockBehaviors;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.behavior.*;
import org.cloudburstmc.server.item.ItemUtils;

@Slf4j
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

    public static final Float GET_FUEL_DURATION = 0.0F;

    public static final MineBlockBehavior MINE_BLOCK = (behavior, itemStack, block, owner) -> {
        if (block.getBehaviors().get(BlockBehaviors.CAN_DAMAGE_ITEM)) {
            return behavior.get(ItemBehaviors.ON_DAMAGE).execute(itemStack, 2, owner);
        }
        return itemStack;
    };

    public static final DamageItemBehavior ON_DAMAGE = (behavior, itemStack, damage, owner) -> {
        if (damage <= 0 || ItemUtils.isNull(itemStack) || behavior.get(ItemBehaviors.GET_MAX_DAMAGE).execute() < 0 ||
                !owner.isAlive() || itemStack.get(ItemKeys.UNBREAKABLE) != Boolean.TRUE) {
            if (damage < 0) {
                log.debug("Tried to damage {} with a negative value of {}", itemStack, damage);
            }
            return itemStack;
        }

        Enchantment enchantment = itemStack.get(ItemKeys.ENCHANTMENTS).get(EnchantmentTypes.UNBREAKING);
        int enchantmentLevel = enchantment == null ? 0 : enchantment.level();

        int damageChance = behavior.get(ItemBehaviors.GET_DAMAGE_CHANCE).execute(enchantmentLevel);

        if (enchantmentLevel > 0 && damageChance <= 100) {
            return itemStack;
        }

        Integer damageKey = itemStack.get(ItemKeys.DAMAGE);
        int damageValue = damageKey == null ? 0 : damageKey;

        damageValue += damage;

        if (damageValue >= behavior.get(ItemBehaviors.GET_MAX_DAMAGE).execute()) {
            // TODO: Make the break sound
            return ItemStack.AIR;
        }

        return itemStack.toBuilder()
                .data(ItemKeys.DAMAGE, damageValue)
                .build();
    };

    public static final UseOnBehavior USE_ON = (behavior, itemStack, entity, blockPosition, face, clickPosition) -> {
        return itemStack;
    };
}
