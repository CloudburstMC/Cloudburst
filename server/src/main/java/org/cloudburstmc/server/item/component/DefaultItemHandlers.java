package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.*;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@UtilityClass
public class DefaultItemHandlers {

    public static final DestroySpeedHandler GET_DESTROY_SPEED = (itemStack, block) -> 1.0F;

    public static final FloatItemHandler GET_DESTROY_SPEED_BONUS = (itemStack) -> {
        if (itemStack.isEmpty()) {
            return 1.0F;
        }
        Enchantment enchantment = itemStack.get(ItemKeys.ENCHANTMENTS).get(EnchantmentTypes.EFFICIENCY);
        if (enchantment != null && enchantment.level() > 0) {
            return (enchantment.level() * enchantment.level()) + 1.0F;
        }
        return 0F;
    };

    public static final DamageChanceHandler GET_DAMAGE_CHANCE = unbreakingLevel -> 100f / (unbreakingLevel + 1);

    public static final MineBlockHandler MINE_BLOCK = (itemStack, block, owner) -> {
        if (block.getComponent(BlockComponents.CAN_DAMAGE_ITEM)) {
            DamageItemHandler onDamage = CloudItemRegistry.get().getComponent(itemStack.getType(), ItemComponents.ON_DAMAGE);
            if (onDamage != null) {
                return onDamage.execute(itemStack, 2, owner);
            }
        }
        return itemStack;
    };

    public static final DamageItemHandler ON_DAMAGE = (itemStack, damage, owner) -> {
        IntItemHandler getMaxDamage = CloudItemRegistry.get().getComponent(itemStack.getType(), ItemComponents.GET_MAX_DAMAGE);
        int maxDamage = getMaxDamage != null ? getMaxDamage.execute(itemStack) : -1;

        if (damage <= 0 || itemStack.isEmpty() || maxDamage <= 0 ||
                !owner.isAlive() || itemStack.get(ItemKeys.UNBREAKABLE) == Boolean.TRUE) {
            if (damage < 0) {
                log.debug("Tried to damage {} with a negative value of {}", itemStack, damage);
            }

            return itemStack;
        }

        Enchantment enchantment = itemStack.get(ItemKeys.ENCHANTMENTS).get(EnchantmentTypes.UNBREAKING);
        int enchantmentLevel = enchantment == null ? 0 : enchantment.level();

        DamageChanceHandler getDamageChance = CloudItemRegistry.get().getComponent(itemStack.getType(), ItemComponents.GET_DAMAGE_CHANCE);
        float damageChance = getDamageChance != null ? getDamageChance.execute(enchantmentLevel) : 100;

        damageChance = Math.clamp(damageChance, 0, 100);
        int appliedDamage = 0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < damage; i++) {
            if (random.nextFloat(100) < damageChance) {
                appliedDamage++;
            }
        }

        if (appliedDamage == 0) {
            return itemStack;
        }

        Integer damageKey = itemStack.get(ItemKeys.DAMAGE);
        int damageValue = damageKey == null ? 0 : damageKey;

        damageValue += appliedDamage;

        if (damageValue >= maxDamage) {
            // TODO: Make the break sound
            return ItemStack.EMPTY;
        }

        return itemStack.toBuilder()
                .data(ItemKeys.DAMAGE, damageValue)
                .build();
    };

    public static final UseOnHandler USE_ON = (itemStack, entity, blockPosition, face, clickPosition) -> {
        return itemStack;
    };

    /**
     * Allows placement only on blocks explicitly listed in the item's {@code CAN_PLACE_ON} data.
     * Items with an empty or absent list cannot be placed on any block.
     */
    public static final CanBePlacedOnHandler CAN_BE_PLACED_ON = (item, block) -> {
        List<BlockType> whitelist = item.get(ItemKeys.CAN_PLACE_ON);
        return !whitelist.isEmpty() && whitelist.contains(block.getState().getType());
    };

    /**
     * Allows breaking only blocks explicitly listed in the item's {@code CAN_DESTROY} data.
     * Items with an empty or absent list cannot break any block.
     */
    public static final CanDestroyHandler CAN_DESTROY = (item, block) -> {
        List<BlockType> whitelist = item.get(ItemKeys.CAN_DESTROY);
        return !whitelist.isEmpty() && whitelist.contains(block.getState().getType());
    };
}
