package org.cloudburstmc.server.block.component;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockLayerRules;
import org.cloudburstmc.server.block.BlockLayers;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SeaPickleBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i ORIGIN = Vector3i.from(0, 64, 0);

    @Test
    void stacksUpToFourWhileCrouchingAndDropsTheWholeCluster() {
        Player sneaking = InterfaceProxy.create(Player.class, Map.of("isSneaking", true));

        for (int count = 0; count < 4; count++) {
            BlockState state = BlockStates.SEA_PICKLE.withTrait(BlockTraits.CLUSTER_COUNT, count);
            Block block = InterfaceProxy.create(Block.class, Map.of("getState", state));

            assertEquals(count < 3, SeaPickleBlockHandlers.CAN_BE_REPLACED.execute(block, BlockStates.SEA_PICKLE, null, Direction.UP, Vector3f.ZERO));
            assertEquals(count < 3, SeaPickleBlockHandlers.CAN_BE_REPLACED.execute(block, BlockStates.SEA_PICKLE, sneaking, Direction.UP, Vector3f.ZERO));
            assertFalse(SeaPickleBlockHandlers.CAN_BE_REPLACED.execute(block, BlockStates.STONE, null, Direction.UP, Vector3f.ZERO));

            if (count < 3) {
                BlockState stacked = SeaPickleBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(BlockStates.SEA_PICKLE, block, sneaking, Direction.UP, Vector3f.ZERO);
                assertNotNull(stacked);
                assertEquals(count + 1, stacked.ensureTrait(BlockTraits.CLUSTER_COUNT));
            }

            List<ItemStack> drops = SeaPickleBlockHandlers.GET_LOOT.execute(block, BlockLootContext.empty(new Random(0)));
            assertEquals(1, drops.size());
            assertEquals(count + 1, drops.getFirst().getCount());
            assertSame(ItemTypes.SEA_PICKLE, drops.getFirst().getType());
        }

        assertSame(SeaPickleBlockHandlers.FERTILIZE, REGISTRY.requireComponent(BlockTypes.SEA_PICKLE, BlockComponents.FERTILIZE));
    }

    @Test
    void synchronizesDryStateAndLightWhenAddingOrRemovingSourceWater() {
        for (int count = 0; count < 4; count++) {
            BlockState state = BlockStates.SEA_PICKLE.withTrait(BlockTraits.CLUSTER_COUNT, count);
            BlockLayers dry = Objects.requireNonNull(BlockLayerRules.resolveReplacement(new BlockLayers(BlockStates.AIR, BlockStates.AIR), BlockLayer.PRIMARY, state));
            BlockLayers wet = Objects.requireNonNull(BlockLayerRules.resolveReplacement(dry, BlockLayer.SECONDARY, BlockStates.WATER));
            BlockLayers drained = Objects.requireNonNull(BlockLayerRules.resolveReplacement(wet, BlockLayer.SECONDARY, BlockStates.AIR));
            BlockLayers broken = Objects.requireNonNull(BlockLayerRules.resolveReplacement(wet, BlockLayer.PRIMARY, BlockStates.AIR));

            assertTrue(dry.primary().ensureTrait(BlockTraits.IS_DEAD));
            assertEquals(0, dry.primary().getLightEmission());
            assertFalse(wet.primary().ensureTrait(BlockTraits.IS_DEAD));
            assertEquals(6 + count * 3, wet.primary().getLightEmission());
            assertEquals(dry, drained);
            assertSame(BlockStates.WATER, broken.primary());
            assertSame(BlockStates.AIR, broken.secondary());
            assertNull(BlockLayerRules.resolveReplacement(dry, BlockLayer.SECONDARY, BlockStates.LAVA));
        }
    }

    @Test
    void waterloggingTakesPrecedenceOverTheLiquidBreakReaction() {
        BlockState pickle = BlockStates.SEA_PICKLE.withTrait(BlockTraits.IS_DEAD, true);
        LiquidState water = LiquidState.of(BlockStates.WATER);
        LiquidState lava = LiquidState.of(BlockStates.LAVA);

        assertFalse(BlockLayerRules.requiresBlockRemoval(pickle, water));
        assertTrue(BlockLayerRules.requiresBlockRemoval(pickle, lava));
        assertTrue(BlockLayerRules.requiresBlockRemoval(BlockStates.DANDELION, water));
        assertFalse(BlockLayerRules.requiresBlockRemoval(pickle, LiquidState.empty()));
    }

    @Test
    void placementPreservesSourceWaterButDoesNotCreateItOnLand() {
        for (BlockState replaced : new BlockState[]{BlockStates.AIR, BlockStates.WATER, BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 1)}) {
            LiquidState liquid = LiquidState.of(replaced);
            Block target = InterfaceProxy.create(Block.class, Map.of("getState", replaced, "getLiquid", liquid));

            BlockState proposed = SeaPickleBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(BlockStates.SEA_PICKLE, target, null, Direction.UP, Vector3f.ZERO);
            assertNotNull(proposed);

            BlockLayers placed = Objects.requireNonNull(BlockLayerRules.resolveReplacement(new BlockLayers(replaced, BlockStates.AIR), BlockLayer.PRIMARY, proposed));
            assertEquals(!liquid.isSource(), placed.primary().ensureTrait(BlockTraits.IS_DEAD));
            assertSame(liquid.isSource() ? BlockStates.WATER : BlockStates.AIR, placed.secondary());
        }
    }

    @Test
    void acceptsPartialTopSurfaceSupportButRejectsAnEmptySurface() {
        Server server = InterfaceProxy.create(Server.class, Map.of("getBlockRegistry", REGISTRY));
        Level level = InterfaceProxy.create(Level.class, Map.of("getServer", server, "getBlockState", BlockStates.AIR));

        for (boolean supported : new boolean[]{false, true}) {
            Block below = InterfaceProxy.create(Block.class, Map.of("getPosition", ORIGIN.add(0, -1, 0), "getCollisionShape", supported ? CloudVoxelShapes.box(0, 0, 0, 0.25f, 1, 0.25f) : CloudVoxelShapes.empty()));
            Block target = InterfaceProxy.create(Block.class, Map.of("getSide", below, "getLevel", level));
            assertEquals(supported, SeaPickleBlockHandlers.CAN_SURVIVE.execute(target));
        }
    }

    @Test
    void fertilizesTheVanillaDiamondWithoutMutatingTheWorld() {
        Level level = InterfaceProxy.create(Level.class,
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getLoadedBlock")) {
                        Vector3i position = (Vector3i) arguments[0];
                        int floor = ORIGIN.getY() - 1 - Math.floorMod(position.getX() + position.getZ(), 2);
                        BlockState state = position.getY() <= floor ? BlockStates.BRAIN_CORAL_BLOCK : BlockStates.WATER;
                        BlockState support = position.getY() - 1 <= floor ? BlockStates.BRAIN_CORAL_BLOCK : BlockStates.WATER;

                        if (position.equals(ORIGIN.add(1, -1, 0))) {
                            state = BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 1);
                        }

                        return pickleTarget((Level) proxy, position, state, support);
                    }

                    throw new AssertionError("Unexpected world access: " + method);
                });

        Block source = pickleTarget(level, ORIGIN, BlockStates.SEA_PICKLE, BlockStates.BRAIN_CORAL_BLOCK);

        FertilizationResult result = SeaPickleBlockHandlers.FERTILIZE.execute(source, null, new SuccessfulGrowthRandom());

        assertEquals(13, result.changes().size());
        assertTrue(result.drops().isEmpty());

        for (BlockChange change : result.changes()) {
            Vector3i offset = change.block().getPosition().sub(ORIGIN);

            assertTrue(Math.abs(offset.getX()) + Math.abs(offset.getZ()) <= 2);
            assertTrue(offset.getY() == -1 || offset.getY() == 0);
            assertSame(BlockTypes.SEA_PICKLE, change.newState().getType());
        }

        BlockChange flowingWaterGrowth = result.changes().stream()
                .filter(change -> change.block().getPosition().equals(ORIGIN.add(1, -1, 0)))
                .findFirst().orElseThrow();
        BlockLayers grown = Objects.requireNonNull(BlockLayerRules.resolveReplacement(
                new BlockLayers(flowingWaterGrowth.block().getState(), BlockStates.AIR),
                BlockLayer.PRIMARY, flowingWaterGrowth.newState()));

        assertSame(BlockStates.WATER, grown.secondary());
        assertFalse(grown.primary().ensureTrait(BlockTraits.IS_DEAD));
        assertSame(source, result.changes().getLast().block());
        assertEquals(3, result.changes().getLast().newState().ensureTrait(BlockTraits.CLUSTER_COUNT));
        assertEquals(0, source.getState().ensureTrait(BlockTraits.CLUSTER_COUNT));
    }

    @Test
    void doesNotSpreadIntoOccupiedBlocksOrOntoDeadCoral() {
        Block existingPickle = pickleTarget(null, ORIGIN.add(1, 0, 0), BlockStates.SEA_PICKLE, BlockStates.BRAIN_CORAL_BLOCK);
        Block lava = pickleTarget(null, ORIGIN.add(1, 0, 0), BlockStates.LAVA, BlockStates.BRAIN_CORAL_BLOCK);
        Block air = pickleTarget(null, ORIGIN.add(1, 0, 0), BlockStates.AIR, BlockStates.BRAIN_CORAL_BLOCK);
        Block deadCoral = pickleTarget(null, ORIGIN.add(1, 0, 0), BlockStates.WATER, BlockStates.DEAD_BRAIN_CORAL_BLOCK);

        for (Level level : new Level[]{
                InterfaceProxy.create(Level.class, Map.of("getLoadedBlock", existingPickle)),
                InterfaceProxy.create(Level.class, Map.of("getLoadedBlock", lava)),
                InterfaceProxy.create(Level.class, Map.of("getLoadedBlock", air)),
                InterfaceProxy.create(Level.class, Map.of("getLoadedBlock", deadCoral)),
                InterfaceProxy.create(Level.class)
        }) {
            Block source = pickleTarget(level, ORIGIN, BlockStates.SEA_PICKLE, BlockStates.BRAIN_CORAL_BLOCK);
            FertilizationResult result = SeaPickleBlockHandlers.FERTILIZE.execute(source, null, new SuccessfulGrowthRandom());

            assertEquals(1, result.changes().size());
            assertSame(source, result.changes().getFirst().block());
            assertEquals(3, result.changes().getFirst().newState().ensureTrait(BlockTraits.CLUSTER_COUNT));
        }
    }

    @Test
    void onlyLivingCoralAndWetPicklesAllowFertilization() {
        Block wetOnDirt = pickleTarget(null, ORIGIN, BlockStates.SEA_PICKLE, BlockStates.DIRT);
        Block wetOnDeadCoral = pickleTarget(null, ORIGIN, BlockStates.SEA_PICKLE, BlockStates.DEAD_BRAIN_CORAL_BLOCK);
        Block dryOnCoral = pickleTarget(null, ORIGIN, BlockStates.SEA_PICKLE.withTrait(BlockTraits.IS_DEAD, true), BlockStates.BRAIN_CORAL_BLOCK);

        assertFalse(SeaPickleBlockHandlers.FERTILIZE.execute(wetOnDirt, null, new Random(0)).succeeded());
        assertFalse(SeaPickleBlockHandlers.FERTILIZE.execute(wetOnDeadCoral, null, new Random(0)).succeeded());
        assertFalse(SeaPickleBlockHandlers.FERTILIZE.execute(dryOnCoral, null, new Random(0)).succeeded());
        assertTrue(BlockStates.BRAIN_CORAL_BLOCK.is(BlockTags.CORAL_BLOCKS));
        assertFalse(BlockStates.DEAD_BRAIN_CORAL_BLOCK.is(BlockTags.CORAL_BLOCKS));
    }

    private static Block pickleTarget(@Nullable Level level, Vector3i position, BlockState state, BlockState support) {
        LiquidState liquid = state.getType().isLiquid() ? LiquidState.of(state)
                : state.getType() == BlockTypes.SEA_PICKLE && !state.ensureTrait(BlockTraits.IS_DEAD)
                ? LiquidState.of(BlockStates.WATER) : LiquidState.empty();

        return InterfaceProxy.create(Block.class,
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getState" -> state;
                    case "getLiquid" -> liquid;
                    case "getPosition" -> position;
                    case "getLevel" -> level;
                    case "getRelativeState" -> support;
                    default -> throw new AssertionError("Unexpected block access: " + method);
                });
    }

    private static class SuccessfulGrowthRandom extends Random {

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
