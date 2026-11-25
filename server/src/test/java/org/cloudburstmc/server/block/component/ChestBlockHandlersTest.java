package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockLootHandler;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ChestBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();

    @Test
    void pairingRequiresCompatibleChestTypesAndMatchingFacing() {
        BlockState chest = BlockStates.CHEST.withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.NORTH);
        BlockState trapped = BlockStates.TRAPPED_CHEST
                .withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.NORTH);
        BlockState copper = BlockTypes.COPPER_CHEST.getDefaultState()
                .withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.NORTH);
        BlockState waxed = BlockTypes.WAXED_OXIDIZED_COPPER_CHEST.getDefaultState()
                .withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.NORTH);

        assertTrue(ChestBlockHandlers.canJoin(chest, chest));
        assertFalse(ChestBlockHandlers.canJoin(chest, trapped));
        assertFalse(ChestBlockHandlers.canJoin(chest, copper));
        assertTrue(ChestBlockHandlers.canJoin(copper, waxed));
        assertFalse(ChestBlockHandlers.canJoin(copper,
                waxed.withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.SOUTH)));
    }

    @Test
    void enderChestDropsObsidianUnlessMinedWithSilkTouch() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        Vector3i position = Vector3i.from(0, 64, 0);
        world.setState(position, BlockStates.ENDER_CHEST);
        BlockLootHandler loot = REGISTRY.requireComponent(BlockTypes.ENDER_CHEST, BlockComponents.GET_LOOT);
        ItemStack pickaxe = ItemStack.from(ItemTypes.DIAMOND_PICKAXE);
        ItemStack silkTouch = pickaxe.toBuilder().setData(ItemDataComponents.ENCHANTMENTS,
                Map.of(EnchantmentTypes.SILK_TOUCH, new Enchantment(EnchantmentTypes.SILK_TOUCH, 1))).build();
        Random random = new Random(0);

        assertEquals(List.of(ItemStack.from(ItemTypes.OBSIDIAN, 8)),
                loot.execute(world.block(position), new BlockLootContext(pickaxe, null, random)));
        assertEquals(List.of(ItemStack.from(ItemTypes.ENDER_CHEST)),
                loot.execute(world.block(position), new BlockLootContext(silkTouch, null, random)));
    }
}
