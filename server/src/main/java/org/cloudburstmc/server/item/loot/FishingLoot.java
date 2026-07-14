package org.cloudburstmc.server.item.loot;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.data.DyeColor;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class FishingLoot {

    private static final WeightedTable<ItemStack> FISH = WeightedTable.<ItemStack>builder()
            .add(ItemStack.from(ItemTypes.COD), 60)
            .add(ItemStack.from(ItemTypes.SALMON), 25)
            .add(ItemStack.from(ItemTypes.TROPICAL_FISH), 2)
            .add(ItemStack.from(ItemTypes.PUFFERFISH), 13)
            .build();

    private static final WeightedTable<ItemStack> TREASURE = WeightedTable.<ItemStack>builder()
            .add(ItemStack.from(ItemTypes.BOW), 1)
            .add(ItemStack.from(ItemTypes.ENCHANTED_BOOK), 1)
            .add(ItemStack.from(ItemTypes.FISHING_ROD), 1)
            .add(ItemStack.from(ItemTypes.NAME_TAG), 1)
            .add(ItemStack.from(ItemTypes.SADDLE), 1)
            .build();

    private static final WeightedTable<ItemStack> JUNK = WeightedTable.<ItemStack>builder()
            .add(ItemStack.from(ItemTypes.BOWL), 120)
            .add(ItemStack.from(ItemTypes.FISHING_ROD), 24)
            .add(ItemStack.from(ItemTypes.LEATHER), 120)
            .add(ItemStack.from(ItemTypes.LEATHER_BOOTS), 120)
            .add(ItemStack.from(ItemTypes.ROTTEN_FLESH), 120)
            .add(ItemStack.from(ItemTypes.STICK), 60)
            .add(ItemStack.from(ItemTypes.STRING), 60)
            .add(ItemStack.from(ItemTypes.POTION), 120)
            .add(ItemStack.from(ItemTypes.BONE), 120)
            .add(ItemStack.builder(ItemTypes.INK_SAC).amount(10).data(ItemKeys.COLOR, DyeColor.BLACK).build(), 12)
            .add(ItemStack.from(BlockTypes.TRIPWIRE_HOOK.getDefaultState()), 120)
            .build();

    public static ItemStack select() {
        return select(0, 0);
    }

    public static ItemStack select(int fortuneLevel, int lureLevel) {
        double treasureChance = clamp(0, 1, 0.05 + 0.01 * fortuneLevel - 0.01 * lureLevel);
        double junkChance = clamp(0, 1, 0.05 - 0.025 * fortuneLevel - 0.01 * lureLevel);
        double fishChance = clamp(0, 1, 1 - treasureChance - junkChance);
        Random random = ThreadLocalRandom.current();

        return selectCategory(fishChance, treasureChance, junkChance, random)
                .flatMap(category -> category.select(random))
                .orElse(ItemStack.EMPTY);
    }

    private static Optional<WeightedTable<ItemStack>> selectCategory(double fishChance, double treasureChance, double junkChance, Random random) {
        WeightedTable.Builder<WeightedTable<ItemStack>> categories = WeightedTable.builder();
        if (fishChance > 0) {
            categories.add(FISH, fishChance);
        }

        if (treasureChance > 0) {
            categories.add(TREASURE, treasureChance);
        }

        if (junkChance > 0) {
            categories.add(JUNK, junkChance);
        }

        return categories.build().select(random);
    }

    private static double clamp(double min, double max, double value) {
        if (value >= max) return max;
        if (value <= min) return min;
        return value;
    }
}
