package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.BlockLootContext;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.BlockLootHandler;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VanillaBlockLoot {

    private static final float[] NORMAL_SAPLING_CHANCES = {0.05f, 0.0625f, 0.083333336f, 0.1f};
    private static final float[] JUNGLE_SAPLING_CHANCES = {0.025f, 0.027777778f, 0.03125f, 0.041666668f, 0.1f};
    private static final float[] STICK_CHANCES = {0.02f, 0.022222223f, 0.025f, 0.033333335f, 0.1f};
    private static final float[] APPLE_CHANCES = {0.005f, 0.0055555557f, 0.00625f, 0.008333334f, 0.025f};

    public static BlockLootHandler silkTouchAlternative(BlockType silkTouchDrop, BlockType normalDrop) {
        requireNonNull(silkTouchDrop, "silkTouchDrop");
        requireNonNull(normalDrop, "normalDrop");
        return (block, context) -> List.of(blockItem(hasSilkTouch(context) ? silkTouchDrop : normalDrop, 1));
    }

    public static BlockLootHandler silkTouchOnly(BlockType drop) {
        requireNonNull(drop, "drop");
        return (block, context) -> hasSilkTouch(context) ? List.of(blockItem(drop, 1)) : List.of();
    }

    public static BlockLootHandler shearsOnly(BlockType drop) {
        requireNonNull(drop, "drop");
        return (block, context) -> hasShears(context) ? List.of(blockItem(drop, 1)) : List.of();
    }

    public static BlockLootHandler shearsOrSilkTouchOnly(BlockType drop) {
        requireNonNull(drop, "drop");
        return (block, context) -> hasShears(context) || hasSilkTouch(context)
                ? List.of(blockItem(drop, 1)) : List.of();
    }

    public static BlockLootHandler grass(BlockType drop) {
        requireNonNull(drop, "drop");
        return (block, context) -> {
            if (hasShears(context)) {
                return List.of(blockItem(drop, 1));
            }

            if (context.random().nextFloat() >= 0.125f) {
                return List.of();
            }

            int fortune = fortuneLevel(context);
            return List.of(ItemStack.from(ItemTypes.WHEAT_SEEDS, 1 + context.random().nextInt(fortune * 2 + 1)));
        };
    }

    public static BlockLootHandler tallGrass() {
        return (block, context) -> {
            if (hasShears(context)) {
                return List.of(blockItem(BlockTypes.SHORT_GRASS, 2));
            }

            return context.random().nextFloat() < 0.125f ? List.of(ItemStack.from(ItemTypes.WHEAT_SEEDS)) : List.of();
        };
    }

    public static BlockLootHandler glowstone(BlockType drop) {
        requireNonNull(drop, "drop");
        return (block, context) -> {
            if (hasSilkTouch(context)) {
                return List.of(blockItem(drop, 1));
            }

            int count = context.random().nextInt(2, 5);
            int fortune = fortuneLevel(context);

            count = Math.min(4, count + context.random().nextInt(fortune + 1));
            return List.of(ItemStack.from(ItemTypes.GLOWSTONE_DUST, count));
        };
    }

    public static BlockLootHandler leaves(BlockType leaves, BlockType sapling, boolean dropsApples) {
        return leaves(leaves, requireNonNull(sapling, "sapling"), dropsApples, NORMAL_SAPLING_CHANCES);
    }

    public static BlockLootHandler jungleLeaves(BlockType leaves, BlockType sapling) {
        return leaves(leaves, requireNonNull(sapling, "sapling"), false, JUNGLE_SAPLING_CHANCES);
    }

    public static BlockLootHandler mangroveLeaves(BlockType leaves) {
        return leaves(leaves, null, false, NORMAL_SAPLING_CHANCES);
    }

    private static BlockLootHandler leaves(BlockType leaves, BlockType sapling, boolean dropsApples, float[] saplingChances) {
        requireNonNull(leaves, "leaves");
        return (block, context) -> {
            if (hasShears(context) || hasSilkTouch(context)) {
                return List.of(blockItem(leaves, 1));
            }

            int fortune = fortuneLevel(context);
            List<ItemStack> drops = new ArrayList<>(3);
            if (sapling != null && passesChance(context, saplingChances, fortune)) {
                drops.add(blockItem(sapling, 1));
            }

            if (passesChance(context, STICK_CHANCES, fortune)) {
                drops.add(ItemStack.from(ItemTypes.STICK, context.random().nextInt(1, 3)));
            }

            if (dropsApples && passesChance(context, APPLE_CHANCES, fortune)) {
                drops.add(ItemStack.from(ItemTypes.APPLE));
            }

            return drops;
        };
    }

    private static boolean hasShears(BlockLootContext context) {
        return !context.tool().isEmpty() && context.tool().getType() == ItemTypes.SHEARS;
    }

    private static boolean hasSilkTouch(BlockLootContext context) {
        return context.enchantmentLevel(EnchantmentTypes.SILK_TOUCH) > 0;
    }

    private static int fortuneLevel(BlockLootContext context) {
        return Math.min(context.enchantmentLevel(EnchantmentTypes.FORTUNE), 255);
    }

    private static boolean passesChance(BlockLootContext context, float[] chances, int fortune) {
        return context.random().nextFloat() < chances[Math.min(fortune, chances.length - 1)];
    }

    private static ItemStack blockItem(BlockType type, int count) {
        return ItemStack.from(type.getDefaultState()).withCount(count);
    }
}
