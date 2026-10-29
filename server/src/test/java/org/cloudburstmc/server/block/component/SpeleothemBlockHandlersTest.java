package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.event.block.BlockSpreadEvent;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.DripstoneThickness;
import org.cloudburstmc.math.vector.Vector3f;
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

class SpeleothemBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 70, 0);
    private static final List<BlockType> TYPES = List.of(BlockTypes.POINTED_DRIPSTONE, BlockTypes.ICICLE, BlockTypes.SULFUR_SPIKE);
    private static final TickBlockHandler SULFUR_GROWTH = SpeleothemBlockHandlers.naturalGrowth(BlockTypes.SULFUR, 2);

    @Test
    void supportRequiresOwnTypeOrASturdyFaceNotJustAnotherSpeleothem() {
        for (BlockType type : TYPES) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            world.setState(POSITION, state(type, true, DripstoneThickness.TIP));
            for (BlockType support : TYPES) {
                world.setState(POSITION.add(0, 1, 0), state(support, true, DripstoneThickness.MIDDLE));
                assertEquals(type == support, SpeleothemBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
            }
            world.setState(POSITION.add(0, 1, 0), BlockStates.STONE);
            assertTrue(SpeleothemBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
            world.unload(POSITION.add(0, 1, 0));
            assertFalse(SpeleothemBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
        }
    }

    @Test
    void placementUsesTheProposedTypeRatherThanAirAtTheTarget() {
        for (BlockType type : TYPES) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            world.setState(POSITION.add(0, 1, 0), state(type, true, DripstoneThickness.TIP));
            BlockState placed = SpeleothemBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(
                    type.getDefaultState(), world.block(POSITION), null, Direction.UP, Vector3f.ZERO);

            assertNotNull(placed);
            assertSame(type, placed.getType());
            assertTrue(placed.ensureTrait(BlockTraits.IS_HANGING));
        }
    }

    @Test
    void thicknessUsesAllSpeleothemsInBothDirections() {
        for (BlockType type : TYPES) {
            for (BlockType adjacent : TYPES) {
                BlockTestWorld world = new BlockTestWorld(REGISTRY);
                world.setState(POSITION, state(type, true, DripstoneThickness.TIP));
                world.setState(POSITION.add(0, -1, 0), state(adjacent, true, DripstoneThickness.TIP));
                updateFront(world);
                assertEquals(DripstoneThickness.FRUSTUM, world.state(POSITION).ensureTrait(BlockTraits.DRIPSTONE_THICKNESS));

                world.setState(POSITION.add(0, -1, 0), state(adjacent, true, DripstoneThickness.MIDDLE));
                world.setState(POSITION.add(0, 1, 0), state(adjacent, true, DripstoneThickness.MIDDLE));
                updateFront(world);
                assertEquals(DripstoneThickness.MIDDLE, world.state(POSITION).ensureTrait(BlockTraits.DRIPSTONE_THICKNESS));

                world.setState(POSITION.add(0, 1, 0), BlockStates.STONE);
                updateFront(world);
                assertEquals(DripstoneThickness.BASE, world.state(POSITION).ensureTrait(BlockTraits.DRIPSTONE_THICKNESS));
            }
        }
    }

    @Test
    void opposingTipsMergeOnlyWithinTheirOwnTypeAndRespectSneaking() {
        Player sneaking = InterfaceProxy.create(Player.class, Map.of("getPitch", -30f, "isSneaking", true));
        for (BlockType type : TYPES) {
            for (BlockType opposing : TYPES) {
                BlockTestWorld world = new BlockTestWorld(REGISTRY);
                world.setState(POSITION.add(0, 1, 0), BlockStates.STONE);
                world.setState(POSITION.add(0, -1, 0), state(opposing, false, DripstoneThickness.TIP));
                BlockState placed = SpeleothemBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(
                        type.getDefaultState(), world.block(POSITION), null, Direction.DOWN, Vector3f.ZERO);
                assertNotNull(placed);
                assertEquals(type == opposing ? DripstoneThickness.MERGE : DripstoneThickness.TIP,
                        placed.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS));

                placed = SpeleothemBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(
                        type.getDefaultState(), world.block(POSITION), sneaking, Direction.DOWN, Vector3f.ZERO);
                assertNotNull(placed);
                assertEquals(DripstoneThickness.TIP, placed.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS));
            }
        }
    }

    @Test
    void onlySupportChangesScheduleFallingAndPendingFallsPreserveThickness() {
        for (BlockType type : TYPES) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            BlockState original = state(type, true, DripstoneThickness.TIP);
            world.setState(POSITION, original);
            updateFront(world);
            assertTrue(world.getScheduledUpdates().isEmpty());

            SpeleothemBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(0, 1, 0)));
            assertEquals(Map.of(POSITION, 2), world.getScheduledUpdates());
            world.setState(POSITION.add(0, -1, 0), state(type, true, DripstoneThickness.TIP));
            updateFront(world);
            assertSame(original, world.state(POSITION));
        }
    }

    @Test
    void taggedDirectionAndTipChecksRecognizeEveryKnownSpecies() {
        for (BlockType type : TYPES) {
            BlockState tip = state(type, true, DripstoneThickness.TIP);
            assertTrue(SpeleothemBlockHandlers.isWithDirection(tip, true));
            assertFalse(SpeleothemBlockHandlers.isWithDirection(tip, false));
            assertTrue(SpeleothemBlockHandlers.isTip(tip, false));
            BlockState merged = state(type, true, DripstoneThickness.MERGE);
            assertTrue(SpeleothemBlockHandlers.isTip(merged, true));
            assertFalse(SpeleothemBlockHandlers.isTip(merged, false));
        }
        assertFalse(SpeleothemBlockHandlers.isWithDirection(BlockStates.STONE, true));
        assertFalse(SpeleothemBlockHandlers.isTip(BlockStates.STONE, true));
        assertTrue(BlockTypes.MAGMA.is(BlockTags.MELTS_ICICLE_ABOVE));
        assertFalse(BlockTypes.STONE.is(BlockTags.MELTS_ICICLE_ABOVE));
    }

    @Test
    void sulfurGrowthRequiresSulfurSupportButDoesNotRequireWater() {
        BlockTestWorld world = sulfur();
        SULFUR_GROWTH.execute(world.block(POSITION), growthRandom(true));
        assertSame(state(BlockTypes.SULFUR_SPIKE, true, DripstoneThickness.TIP), world.state(POSITION.add(0, -1, 0)));
        assertEquals(1, world.getEvents(BlockSpreadEvent.class).size());

        BlockTestWorld unsupported = sulfur();
        unsupported.setState(POSITION.add(0, 1, 0), BlockStates.STONE);
        SULFUR_GROWTH.execute(unsupported.block(POSITION), growthRandom(true));
        assertTrue(unsupported.getEvents(BlockSpreadEvent.class).isEmpty());
    }

    @Test
    void sulfurGrowthSearchAllowsTwoSegmentsButNotThree() {
        for (int length : new int[]{2, 3}) {
            BlockTestWorld world = sulfur();
            for (int offset = 0; offset < length; offset++) {
                world.setState(POSITION.add(0, -offset, 0), state(BlockTypes.SULFUR_SPIKE, true,
                        offset == length - 1 ? DripstoneThickness.TIP : DripstoneThickness.MIDDLE));
            }
            SULFUR_GROWTH.execute(world.block(POSITION), growthRandom(true));
            assertEquals(length == 2 ? BlockTypes.SULFUR_SPIKE : BlockTypes.AIR, world.state(POSITION.add(0, -length, 0)).getType());
        }
    }

    @Test
    void sulfurGrowthMergesOnlyItsOwnOpposingTipsAndHonorsCancellation() {
        for (BlockType opposing : TYPES) {
            BlockTestWorld world = sulfur();
            BlockState existing = state(opposing, false, DripstoneThickness.TIP);
            world.setState(POSITION.add(0, -1, 0), existing);
            world.setState(POSITION.add(0, -2, 0), BlockStates.STONE);
            SULFUR_GROWTH.execute(world.block(POSITION), growthRandom(true));
            assertEquals(opposing == BlockTypes.SULFUR_SPIKE ? DripstoneThickness.MERGE : DripstoneThickness.TIP,
                    world.state(POSITION).ensureTrait(BlockTraits.DRIPSTONE_THICKNESS));
            assertEquals(opposing == BlockTypes.SULFUR_SPIKE ? DripstoneThickness.MERGE : DripstoneThickness.TIP,
                    world.state(POSITION.add(0, -1, 0)).ensureTrait(BlockTraits.DRIPSTONE_THICKNESS));
            assertSame(opposing, world.state(POSITION.add(0, -1, 0)).getType());
            assertSame(BlockStates.STONE, world.state(POSITION.add(0, -2, 0)));
        }

        BlockTestWorld cancelled = sulfur();
        cancelled.onEvent(BlockSpreadEvent.class, event -> event.setCancelled(true));
        SULFUR_GROWTH.execute(cancelled.block(POSITION), growthRandom(true));
        assertSame(BlockStates.AIR, cancelled.state(POSITION.add(0, -1, 0)));

        BlockState opposing = state(BlockTypes.SULFUR_SPIKE, false, DripstoneThickness.TIP);
        cancelled.setState(POSITION.add(0, -1, 0), opposing);
        SULFUR_GROWTH.execute(cancelled.block(POSITION), growthRandom(true));
        assertSame(opposing, cancelled.state(POSITION.add(0, -1, 0)));
        assertEquals(DripstoneThickness.TIP, cancelled.state(POSITION).ensureTrait(BlockTraits.DRIPSTONE_THICKNESS));
    }

    @Test
    void sulfurCanStartAndExtendStalagmitesWithoutReplacingTheFloor() {
        BlockTestWorld world = sulfur();
        Vector3i floor = POSITION.add(0, -5, 0);
        world.setState(floor, BlockStates.STONE);
        SULFUR_GROWTH.execute(world.block(POSITION), growthRandom(false));
        assertSame(state(BlockTypes.SULFUR_SPIKE, false, DripstoneThickness.TIP), world.state(floor.add(0, 1, 0)));
        assertSame(BlockStates.STONE, world.state(floor));

        SULFUR_GROWTH.execute(world.block(POSITION), growthRandom(false));
        assertSame(state(BlockTypes.SULFUR_SPIKE, false, DripstoneThickness.TIP), world.state(floor.add(0, 2, 0)));
        assertSame(BlockStates.STONE, world.state(floor));
    }

    @Test
    void sulfurGrowthRejectsWaterloggedTipsWetTargetsAndUnloadedPaths() {
        for (int scenario = 0; scenario < 3; scenario++) {
            BlockTestWorld world = sulfur();
            switch (scenario) {
                case 0 -> world.setLiquid(POSITION, LiquidState.of(BlockStates.WATER));
                case 1 -> world.setState(POSITION.add(0, -1, 0), BlockStates.WATER);
                case 2 -> world.unload(POSITION.add(0, -1, 0));
                default -> throw new AssertionError();
            }
            SULFUR_GROWTH.execute(world.block(POSITION), growthRandom(true));
            assertTrue(world.getEvents(BlockSpreadEvent.class).isEmpty());
        }
    }

    @Test
    void sulfurUpwardGrowthStopsAtFluidAndSearchesAtMostTenBlocks() {
        for (int scenario = 0; scenario < 2; scenario++) {
            BlockTestWorld world = sulfur();
            world.setState(POSITION.add(0, -12, 0), BlockStates.STONE);
            if (scenario == 0) {
                world.setState(POSITION.add(0, -5, 0), BlockStates.STONE);
                world.setState(POSITION.add(0, -2, 0), BlockStates.WATER);
            }
            SULFUR_GROWTH.execute(world.block(POSITION), growthRandom(false));
            assertTrue(world.getEvents(BlockSpreadEvent.class).isEmpty());
        }
    }

    @Test
    void sulfurGrowthChanceUsesAStrictThreshold() {
        BlockTestWorld world = sulfur();
        Random boundary = new Random(0) {
            @Override
            public float nextFloat() {
                return 0.011377778f;
            }

            @Override
            public int nextInt(int bound) {
                fail("Growth direction must not be selected at the chance threshold");
                return 0;
            }
        };
        SULFUR_GROWTH.execute(world.block(POSITION), boundary);
        assertTrue(world.getEvents(BlockSpreadEvent.class).isEmpty());
    }

    private static BlockTestWorld sulfur() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, state(BlockTypes.SULFUR_SPIKE, true, DripstoneThickness.TIP));
        world.setState(POSITION.add(0, 1, 0), BlockTypes.SULFUR.getDefaultState());
        return world;
    }

    private static Random growthRandom(boolean downward) {
        return new Random(0) {
            @Override
            public float nextFloat() {
                return 0;
            }

            @Override
            public int nextInt(int bound) {
                assertEquals(2, bound);
                return downward ? 1 : 0;
            }
        };
    }

    private static void updateFront(BlockTestWorld world) {
        SpeleothemBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(0, -1, 0)));
    }

    private static BlockState state(BlockType type, boolean hanging, DripstoneThickness thickness) {
        return type.getDefaultState().withTrait(BlockTraits.IS_HANGING, hanging).withTrait(BlockTraits.DRIPSTONE_THICKNESS, thickness);
    }
}
