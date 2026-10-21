package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.misc.FallingBlock;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.event.entity.EntityInsideBlockEvent;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.EntityInsideBlockScanner;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FrogspawnBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);
    private static final Vector3i BELOW = POSITION.add(0, -1, 0);
    private static final BlockState FROGSPAWN = BlockTypes.FROG_SPAWN.getDefaultState();

    @Test
    void requiresSourceWaterIncludingContainedWater() {
        BlockTestWorld world = world();

        assertTrue(FrogspawnBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));

        for (BlockState support : List.of(BlockStates.AIR, BlockStates.STONE, BlockStates.LAVA,
                BlockTypes.ICE.getDefaultState(), BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 1))) {
            world.setState(BELOW, support);

            assertFalse(FrogspawnBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
        }

        world.setState(BELOW, BlockTypes.MANGROVE_ROOTS.getDefaultState());
        world.setLiquid(BELOW, LiquidState.of(BlockStates.WATER));
        assertTrue(FrogspawnBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));

        world.unload(BELOW);
        assertFalse(FrogspawnBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
    }

    @Test
    void rejectsWaterAtTheFrogspawnAndPreservesItWhenRemoved() {
        BlockTestWorld world = world();
        world.setLiquid(POSITION, LiquidState.of(BlockStates.WATER));
        assertFalse(FrogspawnBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));

        FrogspawnBlockHandlers.ON_STATE_CHANGED.execute(world.block(POSITION));
        assertSame(BlockStates.WATER, world.state(POSITION));
        assertTrue(world.getScheduledDelays().isEmpty());
        assertEquals(List.of(new BlockTestWorld.BlockBreakEffect(POSITION, FROGSPAWN)), world.getBlockBreakEffects());
        assertTrue(world.getDrops().isEmpty());
    }

    @Test
    void schedulesHatchingOnlyOnPlacementNotNeighborChanges() {
        BlockTestWorld world = world();

        REGISTRY.requireComponent(BlockTypes.FROG_SPAWN, BlockComponents.ON_STATE_CHANGED).execute(world.block(POSITION));
        FrogspawnBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(BELOW));

        assertEquals(1, world.getScheduledDelays().size());
        int delay = world.getScheduledDelays().getFirst();
        assertTrue(delay >= 3600 && delay < 12000);
    }

    @Test
    void losingWaterSupportDestroysFrogspawnWithBreakFeedbackAndNoDrops() {
        for (BlockState support : List.of(BlockStates.AIR, BlockStates.STONE)) {
            BlockTestWorld world = world();
            world.setState(BELOW, support);

            FrogspawnBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(BELOW));

            assertSame(BlockStates.AIR, world.state(POSITION));
            assertTrue(world.getDrops().isEmpty());
            assertEquals(List.of(new BlockTestWorld.BlockBreakEffect(POSITION, FROGSPAWN)), world.getBlockBreakEffects());
        }
    }

    @Test
    void unsupportedScheduledTicksDestroyWithoutHatching() {
        BlockTestWorld world = world();
        world.setState(BELOW, BlockStates.AIR);

        FrogspawnBlockHandlers.ON_TICK.execute(world.block(POSITION), new Random(1));
        assertSame(BlockStates.AIR, world.state(POSITION));
        assertTrue(world.getEvents(BlockFadeEvent.class).isEmpty());
        assertEquals(List.of(new BlockTestWorld.BlockBreakEffect(POSITION, FROGSPAWN)), world.getBlockBreakEffects());
        assertTrue(world.getDrops().isEmpty());
    }

    @Test
    void hatchCancellationKeepsFrogspawnAndSuppressesBreakEffects() {
        BlockTestWorld world = world();
        world.onEvent(BlockFadeEvent.class, event -> event.setCancelled(true));

        FrogspawnBlockHandlers.ON_TICK.execute(world.block(POSITION), new Random(1));
        assertSame(FROGSPAWN, world.state(POSITION));
        assertEquals(1, world.getEvents(BlockFadeEvent.class).size());
        assertTrue(world.getBlockBreakEffects().isEmpty());
        assertTrue(world.getDrops().isEmpty());
    }

    @Test
    void hatchRespectsPluginReplacementAndDoesNotSpawnTadpoles() {
        BlockTestWorld world = world();
        world.onEvent(BlockFadeEvent.class, event -> event.setNewState(BlockStates.STONE));

        FrogspawnBlockHandlers.ON_TICK.execute(world.block(POSITION), new Random(1));
        assertSame(BlockStates.STONE, world.state(POSITION));
        assertTrue(world.getBlockBreakEffects().isEmpty());
    }

    @Test
    void hatchDoesNotOverwriteBlockChangesMadeByAnEventListener() {
        BlockTestWorld world = world();
        world.onEvent(BlockFadeEvent.class, event -> world.setState(POSITION, BlockStates.STONE));

        FrogspawnBlockHandlers.ON_TICK.execute(world.block(POSITION), new Random(1));
        assertSame(BlockStates.STONE, world.state(POSITION));
        assertTrue(world.getBlockBreakEffects().isEmpty());
    }

    @Test
    void onlyFallingBlocksDestroyFrogspawnAndInsideEffectsCanBeCancelled() {
        BlockTestWorld world = world();
        Entity other = InterfaceProxy.create(Entity.class, Map.of());
        FallingBlock falling = InterfaceProxy.create(FallingBlock.class, Map.of());

        EntityInsideBlockScanner.applyEffects(world.block(POSITION), other, true);
        assertSame(FROGSPAWN, world.state(POSITION));

        world.onEvent(EntityInsideBlockEvent.class, event -> event.setCancelled(true));
        EntityInsideBlockScanner.applyEffects(world.block(POSITION), falling, true);
        assertSame(FROGSPAWN, world.state(POSITION));
        assertTrue(world.getBlockBreakEffects().isEmpty());

        world.onEvent(EntityInsideBlockEvent.class, event -> event.setCancelled(false));
        EntityInsideBlockScanner.applyEffects(world.block(POSITION), falling, true);
        assertSame(BlockStates.AIR, world.state(POSITION));
        assertEquals(List.of(new BlockTestWorld.BlockBreakEffect(POSITION, FROGSPAWN)), world.getBlockBreakEffects());
        assertTrue(world.getDrops().isEmpty());
    }

    private static BlockTestWorld world() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, FROGSPAWN);
        world.setState(BELOW, BlockStates.WATER);
        return world;
    }
}
