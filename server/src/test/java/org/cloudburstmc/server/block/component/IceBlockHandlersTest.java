package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class IceBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);
    private static final BlockState ICE = BlockTypes.ICE.getDefaultState();

    @Test
    void onlyBlockLightAboveTheThresholdMeltsOrdinaryIce() {
        BlockTestWorld world = world(ICE);
        int threshold = 11 - ICE.getLightDampening();

        IceBlockHandlers.randomTick(world.block(POSITION), threshold, false);
        assertSame(ICE, world.state(POSITION));
        assertTrue(world.getEvents(BlockFadeEvent.class).isEmpty());

        IceBlockHandlers.randomTick(world.block(POSITION), threshold + 1, false);
        assertSame(BlockStates.WATER, world.state(POSITION));
        assertEquals(1, world.getEvents(BlockFadeEvent.class).size());
    }

    @Test
    void fadingCanBeCancelledOrRedirectedAndDoesNotOverwriteListenerChanges() {
        BlockTestWorld cancelled = world(ICE);
        cancelled.onEvent(BlockFadeEvent.class, event -> event.setCancelled(true));
        assertFalse(IceBlockHandlers.melt(cancelled.block(POSITION), true));
        assertSame(ICE, cancelled.state(POSITION));

        BlockTestWorld redirected = world(ICE);
        redirected.onEvent(BlockFadeEvent.class, event -> event.setNewState(BlockStates.STONE));
        assertTrue(IceBlockHandlers.melt(redirected.block(POSITION), false));
        assertSame(BlockStates.STONE, redirected.state(POSITION));

        BlockTestWorld changed = world(ICE);
        changed.onEvent(BlockFadeEvent.class, event -> changed.setState(POSITION, BlockStates.DIRT));
        assertFalse(IceBlockHandlers.melt(changed.block(POSITION), false));
        assertSame(BlockStates.DIRT, changed.state(POSITION));
    }

    @Test
    void meltingInAnEvaporatingDimensionProducesAir() {
        BlockTestWorld world = world(ICE);
        assertTrue(IceBlockHandlers.melt(world.block(POSITION), true));
        assertSame(BlockStates.AIR, world.state(POSITION));
        assertSame(BlockStates.AIR, world.getEvents(BlockFadeEvent.class).getFirst().getNewState());
    }

    @Test
    void ordinaryBreakingProducesWaterOnlyAboveMotionBlockingBlocksOrLiquids() {
        for (BlockState support : List.of(BlockStates.AIR, BlockStates.STONE, BlockStates.WATER, BlockStates.LAVA)) {
            BlockTestWorld world = world(ICE);
            world.setState(POSITION.add(0, -1, 0), support);
            Block original = world.block(POSITION);
            world.setState(POSITION, BlockStates.AIR);

            IceBlockHandlers.afterBreak(original, BlockLootContext.empty(new Random(0)), false);

            assertSame(support == BlockStates.AIR ? BlockStates.AIR : BlockStates.WATER, world.state(POSITION));
            assertTrue(world.getEvents(BlockFadeEvent.class).isEmpty());
        }
    }

    @Test
    void silkTouchCreativeAndEvaporationPreventWaterAfterBreaking() {
        Player creative = InterfaceProxy.create(Player.class, Map.of("isCreative", true));
        List<BlockLootContext> contexts = List.of(
                new BlockLootContext(silkPickaxe(), null, new Random(0)),
                new BlockLootContext(ItemStack.EMPTY, creative, new Random(0)),
                BlockLootContext.empty(new Random(0))
        );

        for (int i = 0; i < contexts.size(); i++) {
            BlockTestWorld world = world(ICE);
            world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
            Block original = world.block(POSITION);
            world.setState(POSITION, BlockStates.AIR);

            IceBlockHandlers.afterBreak(original, contexts.get(i), i == 2);

            assertSame(BlockStates.AIR, world.state(POSITION));
        }
    }

    @Test
    void icePackedIceAndBlueIceRequireSilkTouchButFrostedIceNeverDrops() {
        BlockLootContext ordinary = BlockLootContext.empty(new Random(0));
        BlockLootContext silk = new BlockLootContext(silkPickaxe(), null, new Random(0));

        for (BlockType type : List.of(BlockTypes.ICE, BlockTypes.PACKED_ICE, BlockTypes.BLUE_ICE, BlockTypes.FROSTED_ICE)) {
            BlockTestWorld world = world(type.getDefaultState());
            assertTrue(REGISTRY.requireComponent(type, BlockComponents.GET_LOOT).execute(world.block(POSITION), ordinary).isEmpty());

            List<ItemStack> drops = REGISTRY.requireComponent(type, BlockComponents.GET_LOOT).execute(world.block(POSITION), silk);
            if (type == BlockTypes.FROSTED_ICE) {
                assertTrue(drops.isEmpty());
                assertTrue(REGISTRY.requireComponent(type, BlockComponents.GET_PICK_BLOCK).execute(world.block(POSITION)).isEmpty());
            } else {
                assertEquals(1, drops.size());
                assertSame(type.asItem().orElseThrow(), drops.getFirst().getType());
            }

            assertEquals(type == BlockTypes.ICE, REGISTRY.requireComponent(type, BlockComponents.CAN_RANDOM_TICK));
        }
    }

    @Test
    void iceRetainsItsDataDrivenFriction() {
        assertEquals(0.98f, ICE.getFriction());
        assertEquals(0.98f, BlockTypes.PACKED_ICE.getDefaultState().getFriction());
        assertEquals(0.98f, BlockTypes.FROSTED_ICE.getDefaultState().getFriction());
        assertEquals(0.989f, BlockTypes.BLUE_ICE.getDefaultState().getFriction());
    }

    private static ItemStack silkPickaxe() {
        return ItemStack.builder().itemType(ItemTypes.DIAMOND_PICKAXE)
                .setData(ItemDataComponents.ENCHANTMENTS, Map.of(EnchantmentTypes.SILK_TOUCH, new Enchantment(EnchantmentTypes.SILK_TOUCH, 1)))
                .build();
    }

    private static BlockTestWorld world(BlockState state) {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, state);
        return world;
    }
}
