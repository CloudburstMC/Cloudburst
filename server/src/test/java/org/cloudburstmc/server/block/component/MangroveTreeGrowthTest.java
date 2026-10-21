package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.event.block.BlockFertilizeEvent;
import org.cloudburstmc.api.event.level.StructureGrowEvent;
import org.cloudburstmc.api.level.TreeType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockLayerRules;
import org.cloudburstmc.server.block.BlockLayers;
import org.cloudburstmc.server.item.component.BoneMealItemHandlers;
import org.cloudburstmc.server.level.feature.tree.MangroveTreeGenerator;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MangroveTreeGrowthTest {

    private static final Vector3i ORIGIN = Vector3i.from(0, 64, 0);
    private static final BlockState PLANTED = BlockStates.MANGROVE_PROPAGULE
            .withTrait(BlockTraits.IS_HANGING, false).withTrait(BlockTraits.PROPAGULE_STAGE, 4);

    @Test
    void plansBothTreeFormsWithRootsLogsAndLeavesWithoutChangingTheWorld() {
        for (boolean tall : List.of(false, true)) {
            BlockTestWorld world = world();
            MangroveTreeGenerator.Plan plan = plan(world, tall);

            assertEquals(tall ? TreeType.TALL_MANGROVE : TreeType.MANGROVE, plan.type());
            assertTrue(plan.blocks().stream().anyMatch(change -> change.newState().getType() == BlockTypes.MANGROVE_ROOTS));
            assertTrue(plan.blocks().stream().anyMatch(change -> change.newState().getType() == BlockTypes.MANGROVE_LOG));
            assertTrue(plan.blocks().stream().anyMatch(change -> change.newState().getType() == BlockTypes.MANGROVE_LEAVES));
            assertSame(PLANTED, world.state(ORIGIN));

            for (BlockChange change : plan.blocks()) {
                assertSame(change.block().getState(), world.state(change.block().getPosition()));
            }
        }
    }

    @Test
    void blockedAndUnloadedSpaceCannotConsumeThePropaguleOrPartiallyGenerateATree() {
        BlockTestWorld world = world();
        world.setState(ORIGIN.add(0, 1, 0), BlockStates.STONE);

        for (int seed = 0; seed < 32; seed++) {
            assertNull(MangroveTreeGenerator.generate(world.block(ORIGIN), new Random(seed)));
        }

        assertSame(PLANTED, world.state(ORIGIN));
        world.setState(ORIGIN.add(0, 1, 0), BlockStates.AIR);
        world.unload(ORIGIN.add(0, 1, 0));

        assertNull(MangroveTreeGenerator.generate(world.block(ORIGIN), new Random(0)));
        assertSame(PLANTED, world.state(ORIGIN));
    }

    @Test
    void boneMealCanBeConsumedWithoutGrowthAndNeverGrowsHangingPropagulesIntoTrees() {
        BlockTestWorld world = world();
        Random failedGrowth = new Random(0) {
            @Override
            public float nextFloat() {
                return 0.9f;
            }
        };
        FertilizationResult result = MangrovePropaguleBlockHandlers.FERTILIZE.execute(world.block(ORIGIN), null, failedGrowth);

        assertTrue(result.succeeded());
        assertTrue(result.changes().isEmpty());
        assertTrue(BoneMealItemHandlers.applyFertilization(world.block(ORIGIN), null, result));
        assertSame(PLANTED, world.state(ORIGIN));
        assertTrue(world.getEvents(BlockFertilizeEvent.class).isEmpty());

        world.setState(ORIGIN.add(0, 1, 0), BlockStates.MANGROVE_LEAVES);
        world.setState(ORIGIN, PLANTED.withTrait(BlockTraits.IS_HANGING, true));
        assertFalse(MangrovePropaguleBlockHandlers.FERTILIZE.execute(world.block(ORIGIN), null, new Random(0)).succeeded());
    }

    @Test
    void successfulBoneMealBuildsTheTreeAndPreservesWaterInItsRoots() {
        BlockTestWorld world = world();
        world.setLiquid(ORIGIN, LiquidState.of(BlockStates.WATER));
        FertilizationResult result = FertilizationResult.none();

        for (int seed = 0; seed < 256 && result.treeType() == null; seed++) {
            Random random = new Random(seed) {
                private boolean first = true;

                @Override
                public float nextFloat() {
                    if (this.first) {
                        this.first = false;
                        return 0;
                    }

                    return super.nextFloat();
                }
            };
            result = MangrovePropaguleBlockHandlers.FERTILIZE.execute(world.block(ORIGIN), null, random);
        }

        assertNotNull(result.treeType());
        assertFalse(result.changes().isEmpty());
        BlockState replacement = result.changes().stream()
                .filter(change -> change.block().getPosition().equals(ORIGIN))
                .map(BlockChange::newState).findFirst().orElseThrow();
        BlockLayers layers = BlockLayerRules.resolveReplacement(new BlockLayers(PLANTED, BlockStates.WATER), BlockLayer.PRIMARY, replacement);

        assertNotNull(layers);
        assertTrue(layers.primary() == BlockStates.WATER || layers.secondary() == BlockStates.WATER);
        assertTrue(BoneMealItemHandlers.applyFertilization(world.block(ORIGIN), null, result));
        assertNotSame(PLANTED, world.state(ORIGIN));
        assertTrue(world.block(ORIGIN).getLiquid().getType().isSameFamily(LiquidTypes.WATER));
        assertEquals(1, world.getEvents(StructureGrowEvent.class).size());
        assertEquals(1, world.getEvents(BlockFertilizeEvent.class).size());
    }

    @Test
    void rootsTurnMudIntoMuddyRootsWithoutExcavatingTheGroundBelow() {
        BlockTestWorld world = world();
        for (int x = -24; x <= 24; x++) {
            for (int z = -24; z <= 24; z++) {
                world.setState(Vector3i.from(x, 63, z), BlockStates.MUD);
                world.setState(Vector3i.from(x, 62, z), BlockStates.DIRT);
            }
        }

        MangroveTreeGenerator.Plan plan = plan(world, false);
        assertTrue(plan.blocks().stream().anyMatch(change -> change.newState().getType() == BlockTypes.MUDDY_MANGROVE_ROOTS));
        assertTrue(plan.blocks().stream().noneMatch(change -> change.block().getPosition().getY() <= 62));
    }

    @Test
    void fertilizationForwardsStructureCancellationAndListenerChangesInPaperOrder() {
        BlockTestWorld world = world();
        MangroveTreeGenerator.Plan plan = plan(world, false);
        FertilizationResult result = new FertilizationResult(plan.blocks(), List.of(), true, plan.type());
        List<Class<?>> order = new ArrayList<>();
        world.onEvent(Event.class, event -> {
            order.add(event.getClass());
            if (event instanceof StructureGrowEvent growth) {
                assertTrue(growth.isFromBonemeal());
                growth.setCancelled(true);
            } else if (event instanceof BlockFertilizeEvent fertilize) {
                assertTrue(fertilize.isCancelled());
            }
        });

        assertFalse(BoneMealItemHandlers.applyFertilization(world.block(ORIGIN), null, result));
        assertEquals(List.of(StructureGrowEvent.class, BlockFertilizeEvent.class), order);
        assertSame(PLANTED, world.state(ORIGIN));

        world.onEvent(Event.class, event -> {
            if (event instanceof StructureGrowEvent growth) {
                growth.setCancelled(true);
                growth.getBlocks().clear();
                growth.getBlocks().add(new BlockChange(world.block(ORIGIN), BlockStates.MANGROVE_LOG));
            } else if (event instanceof BlockFertilizeEvent fertilize) {
                fertilize.setCancelled(false);
            }
        });

        assertTrue(BoneMealItemHandlers.applyFertilization(world.block(ORIGIN), null, result));
        assertSame(BlockStates.MANGROVE_LOG, world.state(ORIGIN));
    }

    @Test
    void naturalGrowthFiresOnlyStructureGrowthAndPreservesCancelledPlants() {
        BlockTestWorld world = world();
        world.onEvent(StructureGrowEvent.class, event -> {
            assertFalse(event.isFromBonemeal());
            assertNull(event.getPlayer());
            event.setCancelled(true);
        });

        for (int seed = 0; seed < 256 && world.getEvents(StructureGrowEvent.class).isEmpty(); seed++) {
            MangrovePropaguleBlockHandlers.ON_RANDOM_TICK.execute(world.block(ORIGIN), new Random(seed));
        }

        assertFalse(world.getEvents(StructureGrowEvent.class).isEmpty());
        assertTrue(world.getEvents(BlockFertilizeEvent.class).isEmpty());
        assertSame(PLANTED, world.state(ORIGIN));

        world.onEvent(StructureGrowEvent.class, event -> {});
        for (int seed = 0; seed < 256 && world.state(ORIGIN) == PLANTED; seed++) {
            MangrovePropaguleBlockHandlers.ON_RANDOM_TICK.execute(world.block(ORIGIN), new Random(seed));
        }

        assertNotSame(PLANTED, world.state(ORIGIN));
    }

    @Test
    void staleFertilizationPlansDoNotOverwriteInterveningChanges() {
        BlockTestWorld world = world();
        MangroveTreeGenerator.Plan plan = plan(world, false);
        FertilizationResult result = new FertilizationResult(plan.blocks(), List.of(), true, plan.type());
        BlockChange changed = plan.blocks().stream().filter(change -> !change.block().getPosition().equals(ORIGIN)).findFirst().orElseThrow();
        world.onEvent(BlockFertilizeEvent.class, event -> world.setState(changed.block().getPosition(), BlockStates.STONE));

        assertFalse(BoneMealItemHandlers.applyFertilization(world.block(ORIGIN), null, result));
        assertSame(PLANTED, world.state(ORIGIN));
        assertSame(BlockStates.STONE, world.state(changed.block().getPosition()));
    }

    private static MangroveTreeGenerator.Plan plan(BlockTestWorld world, boolean tall) {
        for (int seed = 0; seed < 256; seed++) {
            MangroveTreeGenerator.Plan plan = MangroveTreeGenerator.generate(world.block(ORIGIN), new VariantRandom(seed, tall));
            if (plan != null) {
                return plan;
            }
        }

        throw new AssertionError("No mangrove could grow in an unobstructed world");
    }

    private static BlockTestWorld world() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        for (int x = -24; x <= 24; x++) {
            for (int z = -24; z <= 24; z++) {
                world.setState(Vector3i.from(x, 63, z), BlockStates.DIRT);
            }
        }

        world.setState(ORIGIN, PLANTED);
        return world;
    }

    private static class VariantRandom extends Random {

        private final boolean tall;
        private boolean first = true;

        private VariantRandom(long seed, boolean tall) {
            super(seed);
            this.tall = tall;
        }

        @Override
        public float nextFloat() {
            if (this.first) {
                this.first = false;
                return this.tall ? 0 : 0.99f;
            }

            return super.nextFloat();
        }
    }
}
