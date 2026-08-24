package org.cloudburstmc.server.container.anvil;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.EnchantmentRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnvilResultCalculator {

    public static final int MAX_NAME_LENGTH = 50;

    public static AnvilResult calculate(AnvilContext context) {
        ItemStack input = context.input();
        if (input.isEmpty()) {
            return AnvilResult.empty();
        }

        if (!canStoreEnchantments(input)) {
            return AnvilResult.empty();
        }

        ItemStack material = context.material();
        ItemStack result = input;
        Map<EnchantmentType, Enchantment> enchantments = new HashMap<>(result.get(ItemKeys.ENCHANTMENTS));

        long repairCostTax = repairCost(input) + repairCost(material);
        int cost = 0;
        int repairItemCountCost = 0;

        if (!material.isEmpty()) {
            if (isDamageable(result) && canRepairWith(result, material)) {
                MaterialRepair materialRepair = repairWithMaterial(result, material);
                if (materialRepair.countCost() == 0) {
                    return AnvilResult.empty();
                }

                result = materialRepair.result();
                cost += materialRepair.levelCost();
                repairItemCountCost = materialRepair.countCost();
            } else {
                boolean sameDamageableItem = isSameDamageableItem(result, material);
                if (!sameDamageableItem && !isEnchantedBook(material)) {
                    return AnvilResult.empty();
                }

                if (sameDamageableItem) {
                    ItemMerge itemMerge = mergeItemDamage(result, material);
                    result = itemMerge.result();
                    cost += itemMerge.levelCost();
                }

                EnchantmentMerge enchantmentMerge = mergeEnchantments(
                        input,
                        material,
                        enchantments,
                        context.hasInfiniteMaterials(),
                        context.bypassEnchantmentLevelRestriction());

                if (!enchantmentMerge.hasCompatibleEnchantment() && enchantmentMerge.hasIncompatibleEnchantment()) {
                    return AnvilResult.empty();
                }

                cost += enchantmentMerge.levelCost();
                enchantments = enchantmentMerge.enchantments();
            }
        }

        RenameResult renameResult = applyRename(input, result, context.renameText());
        result = renameResult.result();
        if (result.isEmpty()) {
            return AnvilResult.empty();
        }

        cost += renameResult.levelCost();
        int renameCost = renameResult.levelCost();

        if (cost <= 0) {
            return AnvilResult.empty();
        }

        int finalCost = clampCost(repairCostTax + cost);
        boolean onlyRenaming = renameCost == cost;
        if (onlyRenaming && finalCost >= context.maximumRepairCost()) {
            finalCost = Math.max(0, context.maximumRepairCost() - 1);
        }

        if (finalCost >= context.maximumRepairCost() && !context.hasInfiniteMaterials()) {
            return AnvilResult.empty(finalCost);
        }

        result = applyResultMetadata(result, material, enchantments, onlyRenaming);
        return new AnvilResult(result, finalCost, repairItemCountCost, onlyRenaming);
    }

    public static int calculateIncreasedRepairCost(int baseCost) {
        return (int) Math.min(baseCost * 2L + 1L, Integer.MAX_VALUE);
    }

    private static MaterialRepair repairWithMaterial(ItemStack input, ItemStack material) {
        ItemStack result = input;
        int maxDamage = maxDamage(input);
        int repairAmount = Math.min(result.getDamage(), maxDamage / 4);
        int countCost = 0;
        int levelCost = 0;

        while (repairAmount > 0 && countCost < material.getCount()) {
            result = result.repair(repairAmount);
            countCost++;
            levelCost++;
            repairAmount = Math.min(result.getDamage(), maxDamage / 4);
        }

        return new MaterialRepair(result, levelCost, countCost);
    }

    private static ItemMerge mergeItemDamage(ItemStack input, ItemStack material) {
        int maxDamage = maxDamage(input);
        int inputRemaining = maxDamage - input.getDamage();
        int materialRemaining = maxDamage - material.getDamage();
        int bonus = maxDamage * 12 / 100;
        int mergedDamage = maxDamage - (inputRemaining + materialRemaining + bonus);
        if (mergedDamage < 0) {
            mergedDamage = 0;
        }

        if (mergedDamage >= input.getDamage()) {
            return new ItemMerge(input, 0);
        }

        return new ItemMerge(input.withDamage(mergedDamage), 2);
    }

    private static EnchantmentMerge mergeEnchantments(ItemStack input, ItemStack material,
                                                      Map<EnchantmentType, Enchantment> currentEnchantments,
                                                      boolean hasInfiniteMaterials,
                                                      boolean bypassLevelRestriction) {
        Map<EnchantmentType, Enchantment> merged = new HashMap<>(currentEnchantments);
        Map<EnchantmentType, Enchantment> materialEnchantments = material.get(ItemKeys.ENCHANTMENTS);
        boolean usingBook = isEnchantedBook(material);
        boolean compatible = false;
        boolean incompatible = false;
        int levelCost = 0;

        for (Enchantment enchantment : materialEnchantments.values()) {
            EnchantmentType type = enchantment.type();
            Enchantment current = merged.get(type);
            int level = current != null && current.level() == enchantment.level()
                    ? enchantment.level() + 1
                    : Math.max(enchantment.level(), current == null ? 0 : current.level());

            Enchantment candidate = new Enchantment(type, level);
            boolean canApply = hasInfiniteMaterials || isEnchantedBook(input) || canEnchant(candidate, input);
            for (Enchantment existing : merged.values()) {
                if (!existing.type().equals(type) && !areCompatible(candidate, existing)) {
                    canApply = false;
                    levelCost++;
                }
            }

            if (!canApply) {
                incompatible = true;
                continue;
            }

            compatible = true;
            if (!bypassLevelRestriction && level > type.maxLevel()) {
                level = type.maxLevel();
            }

            merged.put(type, new Enchantment(type, level));
            int fee = type.anvilCost();
            if (usingBook) {
                fee = Math.max(1, fee / 2);
            }

            levelCost += fee * level;
            if (input.getCount() > 1) {
                levelCost = 40;
            }
        }

        return new EnchantmentMerge(merged, levelCost, compatible, incompatible);
    }

    private static RenameResult applyRename(ItemStack input, ItemStack result, @Nullable String renameText) {
        if (renameText == null) {
            return new RenameResult(result, 0);
        }

        String validatedName = validateName(renameText);
        if (validatedName == null) {
            return new RenameResult(ItemStack.EMPTY, 0);
        }

        ItemStackBuilder builder = result.toBuilder();
        String currentName = input.get(ItemKeys.CUSTOM_NAME);
        if (validatedName.isBlank()) {
            if (currentName == null) {
                return new RenameResult(result, 0);
            }
            return new RenameResult(builder.removeData(ItemKeys.CUSTOM_NAME).build(), 1);
        }

        if (Objects.equals(currentName, validatedName)) {
            return new RenameResult(result, 0);
        }

        return new RenameResult(builder.data(ItemKeys.CUSTOM_NAME, validatedName).build(), 1);
    }

    private static ItemStack applyResultMetadata(ItemStack result, ItemStack material,
                                                 Map<EnchantmentType, Enchantment> enchantments,
                                                 boolean onlyRenaming) {
        ItemStackBuilder builder = result.toBuilder();
        int baseCost = Math.max(repairCost(result), repairCost(material));
        if (!onlyRenaming) {
            baseCost = calculateIncreasedRepairCost(baseCost);
        }

        builder.data(ItemKeys.REPAIR_COST, baseCost);
        if (!enchantments.isEmpty()) {
            builder.data(ItemKeys.ENCHANTMENTS, enchantments);
        }

        return builder.build();
    }

    private static int repairCost(ItemStack item) {
        Integer repairCost = item.get(ItemKeys.REPAIR_COST);
        return repairCost == null ? 0 : Math.max(0, repairCost);
    }

    private static boolean isDamageable(ItemStack item) {
        return !item.isEmpty() && CloudItemRegistry.get().requireComponent(item.getType(),
                ItemComponents.DAMAGEABLE).get();
    }

    private static int maxDamage(ItemStack item) {
        return CloudItemRegistry.get().requireComponent(item.getType(), ItemComponents.GET_MAX_DAMAGE).execute(item);
    }

    private static boolean canRepairWith(ItemStack item, ItemStack material) {
        return CloudItemRegistry.get().requireComponent(item.getType(), ItemComponents.CAN_REPAIR_WITH).execute(item,
                material);
    }

    private static boolean canStoreEnchantments(ItemStack item) {
        return !item.isEmpty()
                && CloudItemRegistry.get().requireComponent(item.getType(),
                ItemComponents.CAN_STORE_ENCHANTMENTS).get();
    }

    private static boolean canEnchant(Enchantment enchantment, ItemStack item) {
        return EnchantmentRegistry.get().canEnchant(enchantment, item);
    }

    private static boolean isSameDamageableItem(ItemStack first, ItemStack second) {
        return first.isSimilar(second) && isDamageable(first);
    }

    private static boolean isEnchantedBook(ItemStack item) {
        return !item.isEmpty()
                && item.getType() != null
                && ItemTypes.ENCHANTED_BOOK.getId().equals(item.getType().getId());
    }

    private static boolean areCompatible(Enchantment first, Enchantment second) {
        return EnchantmentRegistry.get().areCompatible(first, second);
    }

    @Nullable
    private static String validateName(String name) {
        StringBuilder builder = new StringBuilder(name.length());
        name.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint))
                .forEach(builder::appendCodePoint);
        String filteredName = builder.toString();
        return filteredName.length() <= MAX_NAME_LENGTH ? filteredName : null;
    }

    private static int clampCost(long cost) {
        if (cost < 0L) {
            return 0;
        }

        if (cost > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) cost;
    }

    public record AnvilContext(ItemStack input, ItemStack material, @Nullable String renameText,
                               int maximumRepairCost, boolean hasInfiniteMaterials,
                               boolean bypassEnchantmentLevelRestriction) {
    }

    public record AnvilResult(ItemStack result, int repairCost, int repairItemCountCost, boolean onlyRenaming) {

        public static AnvilResult empty() {
            return empty(0);
        }

        public static AnvilResult empty(int repairCost) {
            return new AnvilResult(ItemStack.EMPTY, repairCost, 0, false);
        }
    }

    private record MaterialRepair(ItemStack result, int levelCost, int countCost) {
    }

    private record ItemMerge(ItemStack result, int levelCost) {
    }

    private record EnchantmentMerge(Map<EnchantmentType, Enchantment> enchantments, int levelCost,
                                    boolean hasCompatibleEnchantment, boolean hasIncompatibleEnchantment) {
    }

    private record RenameResult(ItemStack result, int levelCost) {
    }
}
