package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.block.BlockLootContext;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;

import java.util.List;
import java.util.random.RandomGenerator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Defines the server-side loot policy shared by equivalent ore block states.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class OreLoot {
    private final ItemStack resource;
    private final ItemStack silkTouchResource;
    private final int minimumCount;
    private final int maximumCount;
    private final int minimumExperience;
    private final int maximumExperience;
    private final OreFortuneMode fortuneMode;

    public static OreLoot coal(BlockState silkTouchResource) {
        return ore(ItemStack.from(ItemTypes.COAL), silkTouchResource, 1, 1, 0, 2);
    }

    public static OreLoot copper(BlockState silkTouchResource) {
        return ore(ItemStack.from(ItemTypes.RAW_COPPER), silkTouchResource, 2, 5, 0, 0);
    }

    public static OreLoot diamond(BlockState silkTouchResource) {
        return ore(ItemStack.from(ItemTypes.DIAMOND), silkTouchResource, 1, 1, 3, 7);
    }

    public static OreLoot emerald(BlockState silkTouchResource) {
        return ore(ItemStack.from(ItemTypes.EMERALD), silkTouchResource, 1, 1, 3, 7);
    }

    public static OreLoot gold(BlockState silkTouchResource) {
        return ore(ItemStack.from(ItemTypes.RAW_GOLD), silkTouchResource, 1, 1, 0, 0);
    }

    public static OreLoot iron(BlockState silkTouchResource) {
        return ore(ItemStack.from(ItemTypes.RAW_IRON), silkTouchResource, 1, 1, 0, 0);
    }

    public static OreLoot lapis(BlockState silkTouchResource) {
        return ore(ItemStack.from(ItemTypes.LAPIS_LAZULI), silkTouchResource, 4, 9, 2, 5);
    }

    public static OreLoot netherGold(BlockState silkTouchResource) {
        return ore(ItemStack.from(ItemTypes.GOLD_NUGGET), silkTouchResource, 2, 6, 0, 1);
    }

    public static OreLoot quartz(BlockState silkTouchResource) {
        return ore(ItemStack.from(ItemTypes.QUARTZ), silkTouchResource, 1, 1, 2, 5);
    }

    public static OreLoot redstone(BlockState silkTouchResource) {
        return create(ItemStack.from(ItemTypes.REDSTONE), silkTouchResource, 4, 5, 1, 5,
                OreFortuneMode.UNIFORM);
    }

    public List<ItemStack> drops(BlockLootContext context) {
        if (context.enchantmentLevel(EnchantmentTypes.SILK_TOUCH) > 0) {
            return List.of(this.silkTouchResource);
        }

        int fortuneLevel = Math.min(context.enchantmentLevel(EnchantmentTypes.FORTUNE), 255);
        int count = count(context.random(), fortuneLevel);
        return List.of(this.resource.withCount(count));
    }

    public int experience(BlockLootContext context) {
        if (context.enchantmentLevel(EnchantmentTypes.SILK_TOUCH) > 0) {
            return 0;
        }
        return nextInclusive(context.random(), this.minimumExperience, this.maximumExperience);
    }

    private static OreLoot ore(ItemStack resource, BlockState silkTouchResource,
                               int minimumCount, int maximumCount,
                               int minimumExperience, int maximumExperience) {
        return create(resource, silkTouchResource, minimumCount, maximumCount,
                minimumExperience, maximumExperience, OreFortuneMode.ORE);
    }

    private static OreLoot create(ItemStack resource, BlockState silkTouchResource,
                                  int minimumCount, int maximumCount,
                                  int minimumExperience, int maximumExperience,
                                  OreFortuneMode fortuneMode) {
        checkNotNull(resource, "resource");
        checkArgument(!resource.isEmpty(), "resource must not be empty");
        checkNotNull(silkTouchResource, "silkTouchResource");
        checkArgument(minimumCount > 0, "minimumCount must be positive");
        checkArgument(maximumCount >= minimumCount, "maximumCount must not be less than minimumCount");
        checkArgument(minimumExperience >= 0, "minimumExperience must not be negative");
        checkArgument(maximumExperience >= minimumExperience,
                "maximumExperience must not be less than minimumExperience");
        return new OreLoot(resource, blockItem(silkTouchResource), minimumCount, maximumCount,
                minimumExperience, maximumExperience, fortuneMode);
    }

    private static ItemStack blockItem(BlockState state) {
        ItemType itemType = ItemTypes.get(state.getType().getId()).orElseThrow(
                () -> new IllegalArgumentException("Block " + state.getType().getId() + " has no item form"));
        return ItemStack.builder()
                .itemType(itemType)
                .data(ItemKeys.BLOCK_STATE, state)
                .build();
    }

    private static int nextInclusive(RandomGenerator random, int minimum, int maximum) {
        return minimum == maximum ? minimum : random.nextInt(minimum, maximum + 1);
    }

    private int count(RandomGenerator random, int fortuneLevel) {
        int count = nextInclusive(random, this.minimumCount, this.maximumCount);
        return this.fortuneMode.apply(random, count, fortuneLevel);
    }
}
