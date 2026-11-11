package org.cloudburstmc.server.container.grindstone;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

@UtilityClass
public class GrindstoneResultCalculator {

    public static boolean accepts(ItemStack item) {
        return item.isEmpty() || needsGrinding(item) || isDamageable(item);
    }

    public static ItemStack calculate(ItemStack input, ItemStack additional) {
        if (input.getCount() > 1 || additional.getCount() > 1) {
            return ItemStack.EMPTY;
        }

        if (input.isEmpty() || additional.isEmpty()) {
            ItemStack item = input.isEmpty() ? additional : input;
            return needsGrinding(item) ? strip(item, ItemStack.EMPTY) : ItemStack.EMPTY;
        }

        if (input.getType() != additional.getType()) {
            return ItemStack.EMPTY;
        }

        int firstMax = maxDamage(input);
        if (firstMax <= 0 || input.isUnbreakable()) {
            int stackLimit = CloudItemRegistry.get().requireComponent(input.getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute(input);
            return stackLimit >= 2 && input.equals(additional) && !enchantments(input).isEmpty() ? strip(input.withCount(2), additional) : ItemStack.EMPTY;
        }

        int secondMax = maxDamage(additional);
        int max = Math.max(firstMax, secondMax);
        long remaining = (long) firstMax - input.getDamage() + secondMax - additional.getDamage() + max / 20;
        return strip(input.withDamage((int) Math.clamp(max - remaining, 0, max)), additional);
    }

    public static int experienceCost(ItemStack input, ItemStack additional) {
        return (int) Math.min(Integer.MAX_VALUE, experienceCost(input) + experienceCost(additional));
    }

    private static long experienceCost(ItemStack item) {
        long cost = 0;
        for (Enchantment enchantment : enchantments(item).values()) {
            if (!enchantment.type().cursed()) {
                cost += enchantment.type().minCost().base() + (long) (enchantment.level() - 1) * enchantment.type().minCost().perLevelAboveFirst();
                if (cost >= Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        return cost;
    }

    private static ItemStack strip(ItemStack input, ItemStack additional) {
        Map<EnchantmentType, Enchantment> curses = new LinkedHashMap<>();
        collectCurses(input, curses);
        collectCurses(additional, curses);

        ItemStackBuilder result = input.toBuilder();
        if (curses.isEmpty()) {
            result.removeData(ItemDataComponents.ENCHANTMENTS).setData(ItemDataComponents.REPAIR_COST, 0);
            if (input.getType() == ItemTypes.ENCHANTED_BOOK) {
                result.itemType(ItemTypes.BOOK);
            }
        } else {
            int repairCost = (int) ((1L << Math.min(31, curses.size())) - 1);
            result.setData(ItemDataComponents.ENCHANTMENTS, curses).setData(ItemDataComponents.REPAIR_COST, repairCost);
        }

        return result.build();
    }

    private static void collectCurses(ItemStack item, Map<EnchantmentType, Enchantment> curses) {
        for (Enchantment enchantment : enchantments(item).values()) {
            if (enchantment.type().cursed()) {
                curses.putIfAbsent(enchantment.type(), enchantment);
            }
        }
    }

    private static Map<EnchantmentType, Enchantment> enchantments(ItemStack item) {
        return item.getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of());
    }

    private static boolean needsGrinding(ItemStack item) {
        if (item.isEmpty()) {
            return false;
        }

        Integer repairCost = item.get(ItemDataComponents.REPAIR_COST);
        return !enchantments(item).isEmpty() || (repairCost == null ? isDamageable(item) : repairCost > 0);
    }

    private static boolean isDamageable(ItemStack item) {
        return !item.isUnbreakable() && maxDamage(item) > 0;
    }

    private static int maxDamage(ItemStack item) {
        return CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.GET_MAX_DAMAGE).execute(item);
    }
}
