package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockLootContext;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class OreLootTest {

    @Test
    void lapisUsesVanillaBaseCountAndExperienceRanges() {
        OreLoot loot = OreLoot.lapis(BlockTypes.LAPIS_ORE.getDefaultState());
        Random random = new Random(1L);

        for (int attempt = 0; attempt < 100; attempt++) {
            BlockLootContext context = new BlockLootContext(ItemStack.EMPTY, null, random);
            int count = loot.drops(context).getFirst().getCount();
            int experience = loot.experience(context);
            assertAll(
                    () -> assertTrue(count >= 4 && count <= 9),
                    () -> assertTrue(experience >= 2 && experience <= 5)
            );
        }
    }

    @Test
    void oreFortuneMultipliesTheBaseCount() {
        OreLoot loot = OreLoot.copper(BlockTypes.COPPER_ORE.getDefaultState());
        Random random = new Random(2L);
        ItemStack tool = enchantedTool(EnchantmentTypes.FORTUNE, 3);

        for (int attempt = 0; attempt < 100; attempt++) {
            int count = loot.drops(new BlockLootContext(tool, null, random)).getFirst().getCount();
            assertTrue(count >= 2 && count <= 20);
        }
    }

    @Test
    void redstoneFortuneAddsAUniformBonus() {
        OreLoot loot = OreLoot.redstone(BlockTypes.REDSTONE_ORE.getDefaultState());
        Random random = new Random(3L);
        ItemStack tool = enchantedTool(EnchantmentTypes.FORTUNE, 3);

        for (int attempt = 0; attempt < 100; attempt++) {
            int count = loot.drops(new BlockLootContext(tool, null, random)).getFirst().getCount();
            assertTrue(count >= 4 && count <= 8);
        }
    }

    @Test
    void redstoneUsesCurrentVanillaExperienceRange() {
        OreLoot loot = OreLoot.redstone(BlockTypes.REDSTONE_ORE.getDefaultState());
        Random random = new Random(5L);

        for (int attempt = 0; attempt < 100; attempt++) {
            int experience = loot.experience(new BlockLootContext(ItemStack.EMPTY, null, random));
            assertTrue(experience >= 1 && experience <= 5);
        }
    }

    @Test
    void litRedstoneSilkTouchProducesTheUnlitOreWithoutExperience() {
        CloudItemRegistry.get();
        OreLoot loot = OreLoot.redstone(BlockTypes.REDSTONE_ORE.getDefaultState());

        ItemStack tool = enchantedTool(EnchantmentTypes.SILK_TOUCH, 1);
        ItemStack resource = loot.drops(new BlockLootContext(tool, null, new Random(4L))).getFirst();

        assertAll(
                () -> assertEquals(ItemTypes.REDSTONE_ORE, resource.getType()),
                () -> assertEquals(Optional.of(BlockTypes.REDSTONE_ORE.getDefaultState()), resource.getBlockState()),
                () -> assertEquals(0, loot.experience(new BlockLootContext(tool, null, new Random(4L))))
        );
    }

    private static ItemStack enchantedTool(EnchantmentType type, int level) {
        CloudItemRegistry.get();
        return ItemStack.builder()
                .itemType(ItemTypes.DIAMOND_PICKAXE)
                .data(ItemKeys.ENCHANTMENTS, Map.of(type, new Enchantment(type, level)))
                .build();
    }
}
