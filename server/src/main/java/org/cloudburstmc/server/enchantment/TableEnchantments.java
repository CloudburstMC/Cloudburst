package org.cloudburstmc.server.enchantment;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.inventory.view.EnchantmentOffer;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.registry.CloudEnchantmentRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

@UtilityClass
public class TableEnchantments {

    public static final int OFFER_COUNT = 3;
    public static final int MAX_BOOKSHELVES = 15;

    public static List<@Nullable EnchantmentOffer> generate(ItemStack item, int bookshelves, int seed) {
        List<@Nullable EnchantmentOffer> offers = new ArrayList<>(Collections.nCopies(OFFER_COUNT, null));
        if (item.isEmpty() || item.getCount() != 1 || !item.getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of()).isEmpty()) {
            return offers;
        }

        CloudItemRegistry items = CloudItemRegistry.get();
        int value = items.requireComponent(item.getType(), ItemBehaviors.GET_ENCHANTABILITY).execute(item);
        if (value <= 0) {
            return offers;
        }

        CloudEnchantmentRegistry enchantments = CloudEnchantmentRegistry.get();
        List<EnchantmentType> eligible = enchantments.values().stream()
                .filter(type -> !type.treasure() && !type.cursed())
                .filter(type -> item.getType() == ItemTypes.BOOK || items.requireComponent(item.getType(), ItemBehaviors.CAN_ENCHANT_WITH).execute(item, type))
                .sorted(Comparator.comparingInt(enchantments::getSerializedId))
                .toList();

        int power = Math.clamp(bookshelves, 0, MAX_BOOKSHELVES);
        Random costRandom = new Random(seed);
        for (int slot = 0; slot < OFFER_COUNT; slot++) {
            int base = costRandom.nextInt(8) + 1 + power / 2 + costRandom.nextInt(power + 1);
            int cost = switch (slot) {
                case 0 -> Math.max(base / 3, 1);
                case 1 -> base * 2 / 3 + 1;
                default -> Math.max(base, power * 2);
            };

            if (cost >= slot + 1) {
                List<Enchantment> selected = select(item, eligible, value, cost, new Random(seed + slot));
                if (!selected.isEmpty()) {
                    offers.set(slot, new EnchantmentOffer(cost, selected));
                }
            }
        }

        return offers;
    }

    private static List<Enchantment> select(ItemStack item, List<EnchantmentType> eligible, int value, int cost, Random random) {
        int modified = cost + 1 + random.nextInt(value / 4 + 1) + random.nextInt(value / 4 + 1);
        float variance = (random.nextFloat() + random.nextFloat() - 1) * 0.15f;
        modified = Math.max(1, Math.round(modified + modified * variance));

        List<Enchantment> available = new ArrayList<>();
        for (EnchantmentType type : eligible) {
            for (int level = type.maxLevel(); level > 0; level--) {
                if (modified >= type.minModifiedCost(level) && modified <= type.maxModifiedCost(level)) {
                    available.add(new Enchantment(type, level));
                    break;
                }
            }
        }

        List<Enchantment> selected = new ArrayList<>();
        if (available.isEmpty()) {
            return selected;
        }

        selected.add(weightedChoice(available, random));
        while (random.nextInt(50) <= modified) {
            Enchantment last = selected.getLast();
            available.removeIf(candidate -> last.type().conflictsWith(candidate.type()));
            if (available.isEmpty()) {
                break;
            }
            selected.add(weightedChoice(available, random));
            modified /= 2;
        }

        if (item.getType() == ItemTypes.BOOK && selected.size() > 1) {
            selected.remove(random.nextInt(selected.size()));
        }

        Collections.swap(selected, 0, random.nextInt(selected.size()));
        return selected;
    }

    private static Enchantment weightedChoice(List<Enchantment> candidates, Random random) {
        int total = candidates.stream().mapToInt(enchantment -> enchantment.type().rarity().getWeight()).sum();
        int choice = random.nextInt(total);
        for (Enchantment candidate : candidates) {
            choice -= candidate.type().rarity().getWeight();
            if (choice < 0) {
                return candidate;
            }
        }
        throw new AssertionError("No weighted enchantment selected");
    }
}
