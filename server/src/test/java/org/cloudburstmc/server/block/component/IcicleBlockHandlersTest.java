package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.event.block.BlockSpreadEvent;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.DripstoneThickness;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class IcicleBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i ROOT = Vector3i.from(0, 70, 0);
    private static final BlockState TIP = icicle(true, DripstoneThickness.TIP);

    @Test
    void placementFallsBackToSupportedOrientationAndMarksOnlyRootAttached() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        assertNull(place(world, ROOT, Direction.UP));

        world.setState(ROOT.add(0, 1, 0), BlockStates.STONE);
        BlockState root = place(world, ROOT, Direction.UP);
        assertNotNull(root);
        assertTrue(root.ensureTrait(BlockTraits.IS_HANGING));
        assertTrue(root.ensureTrait(BlockTraits.IS_ATTACHED));

        world.setState(ROOT, root);
        BlockState extension = place(world, ROOT.add(0, -1, 0), Direction.UP);
        assertNotNull(extension);
        assertTrue(extension.ensureTrait(BlockTraits.IS_HANGING));
        assertFalse(extension.ensureTrait(BlockTraits.IS_ATTACHED));

        world.setState(ROOT, BlockTypes.POINTED_DRIPSTONE.getDefaultState().withTrait(BlockTraits.IS_HANGING, true));
        assertNull(place(world, ROOT.add(0, -1, 0), Direction.UP));
    }

    @Test
    void placementCanMergeOpposingIciclesButNotDripstone() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(ROOT.add(0, 1, 0), BlockStates.PACKED_ICE);
        world.setState(ROOT.add(0, -1, 0), icicle(false, DripstoneThickness.TIP));
        BlockState merged = place(world, ROOT, Direction.DOWN);
        assertNotNull(merged);
        assertTrue(merged.ensureTrait(BlockTraits.IS_HANGING));
        assertEquals(DripstoneThickness.MERGE, merged.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS));

        world.setState(ROOT.add(0, -1, 0), BlockTypes.POINTED_DRIPSTONE.getDefaultState().withTrait(BlockTraits.IS_HANGING, false));
        BlockState unmerged = place(world, ROOT, Direction.DOWN);
        assertNotNull(unmerged);
        assertTrue(unmerged.ensureTrait(BlockTraits.IS_HANGING));
        assertEquals(DripstoneThickness.TIP, unmerged.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS));
    }

    @Test
    void placementKeepsTheRequestedOrientationWhenBothDirectionsHaveSupport() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(ROOT.add(0, 1, 0), BlockStates.PACKED_ICE);
        world.setState(ROOT.add(0, -1, 0), icicle(false, DripstoneThickness.TIP));

        BlockState upright = place(world, ROOT, Direction.UP);
        assertNotNull(upright);
        assertFalse(upright.ensureTrait(BlockTraits.IS_HANGING));
        assertEquals(DripstoneThickness.TIP, upright.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS));

        BlockState hanging = place(world, ROOT, Direction.DOWN);
        assertNotNull(hanging);
        assertTrue(hanging.ensureTrait(BlockTraits.IS_HANGING));
        assertEquals(DripstoneThickness.MERGE, hanging.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS));
    }

    @Test
    void supportLossSchedulesByOrientationAndUnsupportedUprightBreaks() {
        BlockTestWorld hanging = new BlockTestWorld(REGISTRY);
        hanging.setState(ROOT, TIP);
        SpeleothemBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(hanging.block(ROOT), hanging.block(ROOT.add(0, 1, 0)));
        assertEquals(Map.of(ROOT, 2), hanging.getScheduledUpdates());

        BlockTestWorld upright = new BlockTestWorld(REGISTRY);
        upright.setState(ROOT, icicle(false, DripstoneThickness.TIP));
        SpeleothemBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(upright.block(ROOT), upright.block(ROOT.add(0, -1, 0)));
        assertEquals(Map.of(ROOT, 1), upright.getScheduledUpdates());
        SpeleothemBlockHandlers.ON_TICK.execute(upright.block(ROOT), new Random(0));
        assertSame(BlockStates.AIR, upright.state(ROOT));
    }

    @Test
    void growthUsesPackedIceWithoutWaterAndHonorsCancellation() {
        BlockTestWorld world = supplied();
        IcicleBlockHandlers.grow(world.block(ROOT), 4, false);
        assertSame(TIP, world.state(ROOT.add(0, -1, 0)));
        assertEquals(1, world.getEvents(BlockSpreadEvent.class).size());

        BlockTestWorld cancelled = supplied();
        cancelled.onEvent(BlockSpreadEvent.class, event -> event.setCancelled(true));
        IcicleBlockHandlers.grow(cancelled.block(ROOT), 0, false);
        assertSame(BlockStates.AIR, cancelled.state(ROOT.add(0, -1, 0)));

        BlockTestWorld blueIce = supplied();
        blueIce.setState(ROOT.add(0, 1, 0), BlockTypes.BLUE_ICE.getDefaultState());
        IcicleBlockHandlers.grow(blueIce.block(ROOT), 0, false);
        assertTrue(blueIce.getEvents(BlockSpreadEvent.class).isEmpty());
    }

    @Test
    void growthRejectsUprightIcicles() {
        BlockTestWorld world = supplied();
        world.setState(ROOT, icicle(false, DripstoneThickness.TIP));
        world.setState(ROOT.add(0, -1, 0), BlockTypes.PACKED_ICE.getDefaultState());

        IcicleBlockHandlers.grow(world.block(ROOT), 0, false);

        assertTrue(world.getEvents(BlockSpreadEvent.class).isEmpty());
    }

    @Test
    void growthRejectsHeatWaterOpposingTipsAndUnloadedTargets() {
        for (int scenario = 0; scenario < 7; scenario++) {
            BlockTestWorld world = supplied();
            switch (scenario) {
                case 0 -> world.setState(ROOT.add(0, -2, 0), BlockTypes.MAGMA.getDefaultState());
                case 1 -> world.setState(ROOT.add(0, -1, 0), BlockStates.WATER);
                case 2 -> world.setState(ROOT.add(0, -1, 0), icicle(false, DripstoneThickness.TIP));
                case 3 -> world.unload(ROOT.add(0, -1, 0));
                case 6 -> world.setLiquid(ROOT, LiquidState.of(BlockStates.WATER));
                default -> { }
            }
            IcicleBlockHandlers.grow(world.block(ROOT), scenario == 4 ? 5 : 0, scenario == 5);
            assertTrue(world.getEvents(BlockSpreadEvent.class).isEmpty());
        }
    }

    @Test
    void growthSearchAllowsFourSegmentsButNotFive() {
        for (int length : new int[]{4, 5}) {
            BlockTestWorld world = supplied();
            for (int offset = 0; offset < length; offset++) {
                world.setState(ROOT.add(0, -offset, 0), offset == length - 1 ? TIP : icicle(true, DripstoneThickness.MIDDLE));
            }
            IcicleBlockHandlers.grow(world.block(ROOT), 0, false);
            assertEquals(length == 4 ? TIP : BlockStates.AIR, world.state(ROOT.add(0, -length, 0)));
        }
    }

    @Test
    void meltingExemptsRootsAndDropsBlockedTipsInsteadOfFalling() {
        BlockTestWorld root = supplied();
        IcicleBlockHandlers.randomTick(root.block(ROOT), new Random(0), 15, true);
        assertSame(TIP, root.state(ROOT));

        BlockTestWorld extension = supplied();
        extension.setState(ROOT.add(0, -1, 0), TIP);
        extension.setState(ROOT.add(0, -2, 0), BlockStates.STONE);
        IcicleBlockHandlers.randomTick(extension.block(ROOT.add(0, -1, 0)), new Random(0), 5, false);
        assertSame(BlockStates.AIR, extension.state(ROOT.add(0, -1, 0)));
        assertEquals(1, extension.getBreakCount());
    }

    @Test
    void meltingFallsAtLightFiveOrAboveMagmaButNotAtLightFour() {
        BlockTestWorld world = supplied();
        Vector3i tip = ROOT.add(0, -1, 0);
        world.setState(tip, TIP);
        AtomicInteger falls = new AtomicInteger();
        Block block = fallingSpy(world.block(tip), falls);

        IcicleBlockHandlers.randomTick(block, new Random(0), 4, false);
        assertEquals(0, falls.get());
        IcicleBlockHandlers.randomTick(block, new Random(0), 5, false);
        assertEquals(1, falls.get());
        world.setState(tip.add(0, -2, 0), BlockTypes.MAGMA.getDefaultState());
        IcicleBlockHandlers.randomTick(block, new Random(0), 0, false);
        assertEquals(2, falls.get());
        IcicleBlockHandlers.randomTick(block, new Random(0), 0, true);
        assertEquals(3, falls.get());
    }

    private static Block fallingSpy(Block block, AtomicInteger falls) {
        ComplexBlockHandler handler = ignored -> falls.incrementAndGet();
        return InterfaceProxy.create(Block.class, (proxy, method, arguments) -> {
            if (method.getName().equals("requireComponent") && arguments[0] == BlockComponents.START_FALLING) {
                return handler;
            }
            return method.invoke(block, arguments);
        });
    }

    private static BlockState place(BlockTestWorld world, Vector3i position, Direction face) {
        return IcicleBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(TIP, world.block(position), null, face, Vector3f.ZERO);
    }

    private static BlockTestWorld supplied() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(ROOT, TIP);
        world.setState(ROOT.add(0, 1, 0), BlockTypes.PACKED_ICE.getDefaultState());
        return world;
    }

    private static BlockState icicle(boolean hanging, DripstoneThickness thickness) {
        return BlockTypes.ICICLE.getDefaultState().withTrait(BlockTraits.IS_HANGING, hanging)
                .withTrait(BlockTraits.DRIPSTONE_THICKNESS, thickness).withTrait(BlockTraits.IS_ATTACHED, false);
    }
}
