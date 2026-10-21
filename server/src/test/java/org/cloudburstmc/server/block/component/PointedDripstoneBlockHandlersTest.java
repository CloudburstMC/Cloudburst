package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.FallingBlock;
import org.cloudburstmc.api.event.block.BlockFormEvent;
import org.cloudburstmc.api.event.block.BlockSpreadEvent;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.util.BoundingBox;
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
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class PointedDripstoneBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i TIP = Vector3i.from(0, 70, 0);
    private static final BlockState HANGING = pointed(true, DripstoneThickness.TIP);

    @Test
    void placementRequiresSupportAndSelectsTheAvailableOrientation() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        assertNull(PointedDripstoneBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(HANGING, world.block(TIP), null, Direction.UP, Vector3f.ZERO));

        world.setState(TIP.add(0, 1, 0), BlockStates.STONE);
        BlockState placed = PointedDripstoneBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(HANGING, world.block(TIP), null, Direction.UP, Vector3f.ZERO);

        assertNotNull(placed);
        assertTrue(placed.ensureTrait(BlockTraits.IS_HANGING));
        assertEquals(DripstoneThickness.TIP, placed.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS));
    }

    @Test
    void waterTransferSchedulesTravelBeforeFillingACauldron() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        Vector3i cauldron = TIP.add(0, -5, 0);
        world.setState(cauldron, BlockTypes.CAULDRON.getDefaultState());

        PointedDripstoneBlockHandlers.transferFluid(world.block(TIP), 0);

        assertEquals(Map.of(cauldron, 55), world.getScheduledUpdates());
        assertEquals(0, world.state(cauldron).ensureTrait(BlockTraits.FILL_LEVEL));
        assertEquals(List.of(ParticleTypes.STALACTITE_DRIP_WATER), world.getParticles());
        BoundingBox tipShape = world.block(TIP).getOutlineShape().bounds();
        Vector3f expected = TIP.toFloat().add((tipShape.getMinX() + tipShape.getMaxX()) * 0.5f,
                tipShape.getMinY() - 1 / 16f, (tipShape.getMinZ() + tipShape.getMaxZ()) * 0.5f);
        assertEquals(List.of(expected), world.getParticlePositions());

        CauldronBlockHandlers.ON_TICK.execute(world.block(cauldron), new Random(0));
        assertEquals(2, world.state(cauldron).ensureTrait(BlockTraits.FILL_LEVEL));
    }

    @Test
    void transferRejectsFlowingWaterObstructionsAndUnloadedPaths() {
        BlockTestWorld flowing = supplied(BlockTypes.FLOWING_WATER.getDefaultState().withTrait(BlockTraits.LIQUID_DEPTH, 2));
        BlockTestWorld blocked = supplied(BlockStates.WATER);
        BlockTestWorld unloaded = supplied(BlockStates.WATER);

        for (BlockTestWorld world : List.of(flowing, blocked, unloaded)) {
            world.setState(TIP.add(0, -5, 0), BlockTypes.CAULDRON.getDefaultState());
        }
        blocked.setState(TIP.add(0, -2, 0), BlockStates.STONE);
        unloaded.unload(TIP.add(0, -2, 0));

        for (BlockTestWorld world : List.of(flowing, blocked, unloaded)) {
            PointedDripstoneBlockHandlers.transferFluid(world.block(TIP), 0);
            assertTrue(world.getScheduledUpdates().isEmpty());
        }
    }

    @Test
    void onlyTheStartOfAStalactiteTransfersFluidAndLavaHasALowerChance() {
        BlockTestWorld water = supplied(BlockStates.WATER);
        BlockTestWorld lava = supplied(BlockStates.LAVA);
        for (BlockTestWorld world : List.of(water, lava)) {
            world.setState(TIP, pointed(true, DripstoneThickness.FRUSTUM));
            world.setState(TIP.add(0, -1, 0), HANGING);
            world.setState(TIP.add(0, -4, 0), BlockTypes.CAULDRON.getDefaultState());
            PointedDripstoneBlockHandlers.transferFluid(world.block(TIP.add(0, -1, 0)), 0);
            assertTrue(world.getScheduledUpdates().isEmpty());
        }

        PointedDripstoneBlockHandlers.transferFluid(water.block(TIP), 0.1f);
        PointedDripstoneBlockHandlers.transferFluid(lava.block(TIP), 0.1f);

        assertFalse(water.getScheduledUpdates().isEmpty());
        assertTrue(lava.getScheduledUpdates().isEmpty());
    }

    @Test
    void mudConversionHonorsCancellationAndDoesNotFillCauldrons() {
        BlockTestWorld accepted = supplied(BlockTypes.MUD.getDefaultState());
        BlockTestWorld cancelled = supplied(BlockTypes.MUD.getDefaultState());
        cancelled.onEvent(BlockFormEvent.class, event -> event.setCancelled(true));

        PointedDripstoneBlockHandlers.transferFluid(accepted.block(TIP), 0);
        PointedDripstoneBlockHandlers.transferFluid(cancelled.block(TIP), 0);

        assertSame(BlockTypes.CLAY.getDefaultState(), accepted.state(TIP.add(0, 2, 0)));
        assertSame(BlockTypes.MUD.getDefaultState(), cancelled.state(TIP.add(0, 2, 0)));
        assertEquals(1, accepted.getParticles().size());
        assertTrue(cancelled.getParticles().isEmpty());
        assertTrue(accepted.getScheduledUpdates().isEmpty());
    }

    @Test
    void growthRequiresDripstoneAndSourceWaterAndUsesSpreadEvents() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        world.setState(TIP.add(0, 1, 0), BlockTypes.DRIPSTONE_BLOCK.getDefaultState());

        PointedDripstoneBlockHandlers.grow(world.block(TIP), new Random(0));
        assertSame(HANGING, world.state(TIP.add(0, -1, 0)));
        assertEquals(1, world.getEvents(BlockSpreadEvent.class).size());

        BlockTestWorld cancelled = supplied(BlockStates.WATER);
        cancelled.setState(TIP.add(0, 1, 0), BlockTypes.DRIPSTONE_BLOCK.getDefaultState());
        cancelled.onEvent(BlockSpreadEvent.class, event -> event.setCancelled(true));
        PointedDripstoneBlockHandlers.grow(cancelled.block(TIP), new Random(0));

        assertSame(BlockStates.AIR, cancelled.state(TIP.add(0, -1, 0)));
    }

    @Test
    void unsupportedStalagmitesScheduleBreakingAndFallingDamageIsBlockSpecific() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(TIP, pointed(false, DripstoneThickness.TIP));

        PointedDripstoneBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(TIP), world.block(TIP.add(0, -1, 0)));
        assertEquals(Map.of(TIP, 1), world.getScheduledUpdates());

        PointedDripstoneBlockHandlers.ON_TICK.execute(world.block(TIP), new Random(0));
        assertSame(BlockStates.AIR, world.state(TIP));

        FallingBlock falling = InterfaceProxy.create(FallingBlock.class, Map.of());
        assertTrue(REGISTRY.requireComponent(BlockTypes.POINTED_DRIPSTONE, BlockComponents.BREAKS_ON_LANDING));
        assertSame(DamageTypes.FALLING_STALACTITE, REGISTRY.requireComponent(BlockTypes.POINTED_DRIPSTONE,
                BlockComponents.GET_FALLING_DAMAGE_SOURCE).execute(falling).getDamageType());
        assertSame(DamageTypes.FALLING_ANVIL, REGISTRY.requireComponent(BlockTypes.ANVIL,
                BlockComponents.GET_FALLING_DAMAGE_SOURCE).execute(falling).getDamageType());
        assertFalse(REGISTRY.requireComponent(BlockTypes.SAND, BlockComponents.BREAKS_ON_LANDING));
    }

    @Test
    void growthMergesOpposingTipsWithoutReplacingTheirSupports() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        world.setState(TIP.add(0, 1, 0), BlockTypes.DRIPSTONE_BLOCK.getDefaultState());
        world.setState(TIP.add(0, -1, 0), pointed(false, DripstoneThickness.TIP));
        world.setState(TIP.add(0, -2, 0), BlockStates.STONE);

        PointedDripstoneBlockHandlers.grow(world.block(TIP), new Random(0));

        assertSame(pointed(true, DripstoneThickness.MERGE), world.state(TIP));
        assertSame(pointed(false, DripstoneThickness.MERGE), world.state(TIP.add(0, -1, 0)));
        assertSame(BlockStates.STONE, world.state(TIP.add(0, -2, 0)));
        assertEquals(2, world.getEvents(BlockSpreadEvent.class).size());
    }

    @Test
    void stalagmiteGrowthUsesAirAboveTheFloorRatherThanReplacingTheFloor() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        world.setState(TIP.add(0, 1, 0), BlockTypes.DRIPSTONE_BLOCK.getDefaultState());
        world.setState(TIP.add(0, -5, 0), BlockStates.STONE);
        world.setState(TIP.add(0, -6, 0), BlockStates.STONE);
        RandomGenerator upwardGrowth = () -> 0L;

        PointedDripstoneBlockHandlers.grow(world.block(TIP), upwardGrowth);

        assertSame(pointed(false, DripstoneThickness.TIP), world.state(TIP.add(0, -4, 0)));
        assertSame(BlockStates.STONE, world.state(TIP.add(0, -5, 0)));
        assertSame(BlockStates.STONE, world.state(TIP.add(0, -6, 0)));
    }

    private static BlockTestWorld supplied(BlockState source) {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(TIP, HANGING);
        world.setState(TIP.add(0, 1, 0), BlockStates.STONE);
        world.setState(TIP.add(0, 2, 0), source);
        return world;
    }

    private static BlockState pointed(boolean hanging, DripstoneThickness thickness) {
        return BlockTypes.POINTED_DRIPSTONE.getDefaultState().withTrait(BlockTraits.IS_HANGING, hanging)
                .withTrait(BlockTraits.DRIPSTONE_THICKNESS, thickness);
    }
}
