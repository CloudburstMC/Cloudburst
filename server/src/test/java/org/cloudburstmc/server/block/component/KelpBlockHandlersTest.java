package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.event.block.BlockSpreadEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockLayerRules;
import org.cloudburstmc.server.block.BlockLayers;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.VanillaBlockTransformations;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class KelpBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void placementRequiresFullWaterAndRandomizesTheTipAge() {
        for (BlockState water : List.of(BlockStates.WATER, BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 8))) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            world.setState(POSITION, water);
            BlockState placed = KelpBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(kelp(0), world.block(POSITION), null, Direction.UP, Vector3f.ZERO);

            assertNotNull(placed);
            assertTrue(placed.ensureTrait(BlockTraits.KELP_AGE) >= 0);
            assertTrue(placed.ensureTrait(BlockTraits.KELP_AGE) < 25);
        }

        for (BlockState invalid : List.of(BlockStates.AIR, BlockStates.LAVA, BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 1))) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            world.setState(POSITION, invalid);

            assertNull(KelpBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(kelp(0), world.block(POSITION), null, Direction.UP, Vector3f.ZERO));
        }

        BlockTestWorld belowExistingKelp = new BlockTestWorld(REGISTRY);
        belowExistingKelp.setState(POSITION, BlockStates.WATER);
        belowExistingKelp.setState(POSITION.add(0, 1, 0), kelp(4));

        assertSame(kelp(25), KelpBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(kelp(0), belowExistingKelp.block(POSITION), null, Direction.UP, Vector3f.ZERO));
    }

    @Test
    void kelpAndSeagrassContainSourceWaterAndCannotBeDrainedInPlace() {
        for (BlockState plant : List.of(kelp(4), BlockTypes.SEAGRASS.getDefaultState())) {
            BlockLayers layers = Objects.requireNonNull(BlockLayerRules.resolveReplacement(
                    new BlockLayers(BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 3), BlockStates.AIR),
                    BlockLayer.PRIMARY, plant));

            assertSame(plant, layers.primary());
            assertSame(BlockStates.WATER, layers.secondary());
            assertNull(BlockLayerRules.resolveReplacement(layers, BlockLayer.SECONDARY, BlockStates.AIR));
            assertNull(BlockLayerRules.resolveReplacement(layers, BlockLayer.SECONDARY, BlockStates.LAVA));

            BlockLayers broken = Objects.requireNonNull(BlockLayerRules.resolveReplacement(layers, BlockLayer.PRIMARY, BlockStates.AIR));
            assertSame(BlockStates.WATER, broken.primary());
            assertSame(BlockStates.AIR, broken.secondary());
        }
    }

    @Test
    void survivalUsesSturdySupportAndTheKelpSupportExclusionTag() {
        BlockTestWorld world = wetPlant(0);

        for (BlockState support : List.of(BlockStates.STONE, kelp(25))) {
            world.setState(POSITION.add(0, -1, 0), support);
            assertTrue(KelpBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
        }

        for (BlockState support : List.of(BlockStates.AIR, BlockStates.MAGMA)) {
            world.setState(POSITION.add(0, -1, 0), support);
            assertFalse(KelpBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
        }

        KelpBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(0, -1, 0)));
        assertEquals(List.of(1), world.getScheduledDelays());
        assertEquals(0, world.getBreakCount());

        KelpBlockHandlers.ON_TICK.execute(world.block(POSITION), null);
        assertEquals(1, world.getBreakCount());
    }

    @Test
    void naturalGrowthFiresASpreadEventAndConvertsThePreviousTipToABody() {
        BlockTestWorld world = wetPlant(4);
        Vector3i above = POSITION.add(0, 1, 0);
        world.setState(above, BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 3));

        KelpBlockHandlers.ON_RANDOM_TICK.execute(world.block(POSITION), new GrowthRandom());

        assertSame(kelp(5), world.state(above));
        List<BlockSpreadEvent> events = world.getEvents(BlockSpreadEvent.class);

        assertEquals(1, events.size());
        assertEquals(POSITION, events.getFirst().getSource().getPosition());
        assertEquals(above, events.getFirst().getBlock().getPosition());

        KelpBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(above));
        assertSame(kelp(25), world.state(POSITION));
    }

    @Test
    void spreadCancellationReplacementAndListenerChangesAreRespected() {
        Vector3i above = POSITION.add(0, 1, 0);
        BlockTestWorld cancelled = wetPlant(4);
        cancelled.setState(above, BlockStates.WATER);
        cancelled.onEvent(BlockSpreadEvent.class, event -> event.setCancelled(true));

        KelpBlockHandlers.ON_RANDOM_TICK.execute(cancelled.block(POSITION), new GrowthRandom());
        assertSame(BlockStates.WATER, cancelled.state(above));
        assertSame(kelp(4), cancelled.state(POSITION));

        BlockTestWorld replaced = wetPlant(4);
        replaced.setState(above, BlockStates.WATER);
        replaced.onEvent(BlockSpreadEvent.class, event -> event.setNewState(BlockStates.STONE));

        KelpBlockHandlers.ON_RANDOM_TICK.execute(replaced.block(POSITION), new GrowthRandom());
        assertSame(BlockStates.STONE, replaced.state(above));

        BlockTestWorld changed = wetPlant(4);
        changed.setState(above, BlockStates.WATER);
        changed.onEvent(BlockSpreadEvent.class, event -> changed.setState(above, BlockStates.STONE));

        KelpBlockHandlers.ON_RANDOM_TICK.execute(changed.block(POSITION), new GrowthRandom());
        assertSame(BlockStates.STONE, changed.state(above));
    }

    @Test
    void matureTipsStopNaturallyButBoneMealOnTheBodyCanExtendThem() {
        BlockTestWorld world = wetPlant(25);
        Vector3i tip = POSITION.add(0, 1, 0);
        Vector3i above = POSITION.add(0, 2, 0);
        world.setState(tip, kelp(25));
        world.setLiquid(tip, LiquidState.of(BlockStates.WATER));
        world.setState(above, BlockStates.WATER);

        KelpBlockHandlers.ON_RANDOM_TICK.execute(world.block(tip), new GrowthRandom());

        assertSame(BlockStates.WATER, world.state(above));
        assertTrue(world.getEvents(BlockSpreadEvent.class).isEmpty());

        FertilizationResult result = KelpBlockHandlers.FERTILIZE.execute(world.block(POSITION), null, new Random(0));

        assertEquals(1, result.changes().size());
        assertEquals(above, result.changes().getFirst().block().getPosition());
        assertSame(kelp(25), result.changes().getFirst().newState());
        assertSame(BlockStates.WATER, world.state(above));
        assertSame(KelpBlockHandlers.FERTILIZE, REGISTRY.requireComponent(BlockTypes.KELP, BlockComponents.FERTILIZE));
    }

    @Test
    void growthRejectsOccupiedBlocksAndTheBuildHeightBoundary() {
        BlockTestWorld world = wetPlant(0);
        world.setState(POSITION.add(0, 1, 0), BlockStates.STONE);

        KelpBlockHandlers.ON_RANDOM_TICK.execute(world.block(POSITION), new GrowthRandom());
        assertTrue(world.getEvents(BlockSpreadEvent.class).isEmpty());
        assertFalse(KelpBlockHandlers.FERTILIZE.execute(world.block(POSITION), null, new Random(0)).succeeded());

        Vector3i ceiling = Vector3i.from(0, 319, 0);
        world.setState(ceiling, kelp(0));
        world.setLiquid(ceiling, LiquidState.of(BlockStates.WATER));
        world.setState(ceiling.add(0, 1, 0), BlockStates.WATER);

        KelpBlockHandlers.ON_RANDOM_TICK.execute(world.block(ceiling), new GrowthRandom());
        assertTrue(world.getEvents(BlockSpreadEvent.class).isEmpty());
        assertFalse(KelpBlockHandlers.FERTILIZE.execute(world.block(ceiling), null, new Random(0)).succeeded());
    }

    @Test
    void removingTheTipRestartsGrowthBelowAndShearsCanStopItAgain() {
        BlockTestWorld world = wetPlant(25);
        world.setState(POSITION.add(0, 1, 0), BlockStates.WATER);

        KelpBlockHandlers.ON_REMOVE.execute(world.block(POSITION.add(0, 1, 0)));

        BlockState tip = world.state(POSITION);
        assertTrue(tip.ensureTrait(BlockTraits.KELP_AGE) < 25);
        assertSame(kelp(25), VanillaBlockTransformations.SHEARS.get(BlockTypes.KELP).apply(tip, Direction.UP, false));
        assertNull(VanillaBlockTransformations.SHEARS.get(BlockTypes.KELP).apply(kelp(25), Direction.UP, false));

        List<ItemStack> drops = KelpBlockHandlers.GET_LOOT.execute(world.block(POSITION), BlockLootContext.empty(new Random(0)));
        assertEquals(List.of(ItemStack.from(ItemTypes.KELP)), drops);
        assertEquals(ItemStack.from(ItemTypes.KELP), KelpBlockHandlers.GET_PICK_BLOCK.execute(world.block(POSITION)));
    }

    private static BlockTestWorld wetPlant(int age) {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, kelp(age));
        world.setLiquid(POSITION, LiquidState.of(BlockStates.WATER));
        world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
        return world;
    }

    private static BlockState kelp(int age) {
        return BlockTypes.KELP.getDefaultState().withTrait(BlockTraits.KELP_AGE, age);
    }

    private static class GrowthRandom extends Random {

        @Override
        public double nextDouble() {
            return 0;
        }
    }
}
