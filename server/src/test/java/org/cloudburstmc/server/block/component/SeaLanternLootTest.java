package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SeaLanternLootTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void ordinaryMiningDropsTwoOrThreeCrystalsWithAnyTool() {
        Block block = block(BlockTypes.SEA_LANTERN);

        for (ItemStack tool : List.of(ItemStack.EMPTY, ItemStack.from(ItemTypes.NETHERITE_PICKAXE),
                ItemStack.from(ItemTypes.WOODEN_AXE))) {
            for (int baseRoll = 0; baseRoll < 2; baseRoll++) {
                BlockLootContext context = new BlockLootContext(tool, null, new LootRandom(baseRoll, 0));
                List<ItemStack> drops = REGISTRY.requireComponent(BlockTypes.SEA_LANTERN, BlockComponents.GET_LOOT).execute(block, context);

                assertEquals(1, drops.size());
                assertSame(ItemTypes.PRISMARINE_CRYSTALS, drops.getFirst().getType());
                assertEquals(2 + baseRoll, drops.getFirst().getCount());
            }
        }
    }

    @Test
    void fortuneUsesAnAdditiveUniformBonusCappedAtFive() {
        Block block = block(BlockTypes.SEA_LANTERN);

        for (int fortune = 1; fortune <= 3; fortune++) {
            ItemStack tool = fortunePickaxe(fortune);

            for (int baseRoll = 0; baseRoll < 2; baseRoll++) {
                for (int bonus = 0; bonus <= fortune; bonus++) {
                    BlockLootContext context = new BlockLootContext(tool, null, new LootRandom(baseRoll, bonus));
                    List<ItemStack> drops = REGISTRY.requireComponent(BlockTypes.SEA_LANTERN, BlockComponents.GET_LOOT).execute(block, context);

                    assertEquals(1, drops.size());
                    assertSame(ItemTypes.PRISMARINE_CRYSTALS, drops.getFirst().getType());
                    assertEquals(Math.min(5, 2 + baseRoll + bonus), drops.getFirst().getCount());
                }
            }
        }

        BlockLootContext highFortune = new BlockLootContext(fortunePickaxe(255), null, new LootRandom(1, 255));
        List<ItemStack> drops = REGISTRY.requireComponent(BlockTypes.SEA_LANTERN, BlockComponents.GET_LOOT).execute(block, highFortune);
        assertEquals(1, drops.size());
        assertEquals(5, drops.getFirst().getCount());
    }

    @Test
    void silkTouchTakesPrecedenceOverFortuneAndDoesNotRollCrystalCounts() {
        ItemStack tool = ItemStack.builder().itemType(ItemTypes.NETHERITE_PICKAXE)
                .setData(ItemDataComponents.ENCHANTMENTS, Map.of(
                        EnchantmentTypes.SILK_TOUCH, new Enchantment(EnchantmentTypes.SILK_TOUCH, 1),
                        EnchantmentTypes.FORTUNE, new Enchantment(EnchantmentTypes.FORTUNE, 3)))
                .build();
        BlockLootContext context = new BlockLootContext(tool, null, new LootRandom());

        List<ItemStack> drops = REGISTRY.requireComponent(BlockTypes.SEA_LANTERN, BlockComponents.GET_LOOT)
                .execute(block(BlockTypes.SEA_LANTERN), context);

        assertEquals(1, drops.size());
        assertSame(ItemTypes.SEA_LANTERN, drops.getFirst().getType());
        assertEquals(1, drops.getFirst().getCount());
    }

    @Test
    void explosionLootUsesCrystalsBeforeTheSharedExplosionDecay() {
        BlockLootContext context = BlockLootContext.empty(new LootRandom(1, 0));

        List<ItemStack> drops = REGISTRY.requireComponent(BlockTypes.SEA_LANTERN, BlockComponents.GET_EXPLOSION_LOOT)
                .execute(block(BlockTypes.SEA_LANTERN), context);

        assertEquals(1, drops.size());
        assertSame(ItemTypes.PRISMARINE_CRYSTALS, drops.getFirst().getType());
        assertEquals(3, drops.getFirst().getCount());
    }

    @Test
    void sharedLootRulePreservesGlowstoneCountsAndCap() {
        Block block = block(BlockTypes.GLOWSTONE);
        ItemStack tool = fortunePickaxe(3);

        for (int baseRoll = 0; baseRoll < 3; baseRoll++) {
            for (int bonus = 0; bonus <= 3; bonus++) {
                BlockLootContext context = new BlockLootContext(tool, null, new LootRandom(baseRoll, bonus));
                List<ItemStack> drops = REGISTRY.requireComponent(BlockTypes.GLOWSTONE, BlockComponents.GET_LOOT).execute(block, context);

                assertEquals(1, drops.size());
                assertSame(ItemTypes.GLOWSTONE_DUST, drops.getFirst().getType());
                assertEquals(Math.min(4, 2 + baseRoll + bonus), drops.getFirst().getCount());
            }
        }
    }

    private static Block block(BlockType type) {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, type.getDefaultState());
        return world.block(POSITION);
    }

    private static ItemStack fortunePickaxe(int level) {
        return ItemStack.builder().itemType(ItemTypes.NETHERITE_PICKAXE)
                .setData(ItemDataComponents.ENCHANTMENTS, Map.of(EnchantmentTypes.FORTUNE, new Enchantment(EnchantmentTypes.FORTUNE, level)))
                .build();
    }

    private static class LootRandom extends Random {

        private final int[] rolls;
        private int index;

        private LootRandom(int... rolls) {
            this.rolls = rolls;
        }

        @Override
        public int nextInt(int bound) {
            assertTrue(this.index < this.rolls.length, "Unexpected loot roll");
            int roll = this.rolls[this.index++];
            assertTrue(roll >= 0 && roll < bound, "Loot roll outside its bound");
            return roll;
        }
    }
}
