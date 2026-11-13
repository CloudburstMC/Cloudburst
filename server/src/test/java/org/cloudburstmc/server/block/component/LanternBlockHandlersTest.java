package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.event.block.BlockFormEvent;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockLayerRules;
import org.cloudburstmc.server.block.BlockLayers;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class LanternBlockHandlersTest {

    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void placesOnAFloorAndPreservesSourceWater() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(Direction.DOWN.relative(POSITION), BlockStates.STONE);
        world.setState(POSITION, BlockStates.WATER);

        BlockState state = LanternBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(
                BlockTypes.SOUL_LANTERN.getDefaultState(), world.block(POSITION), null, Direction.UP, Vector3f.ZERO
        );

        assertNotNull(state);
        assertFalse(state.ensureTrait(BlockTraits.IS_HANGING));
        BlockLayers layers = BlockLayerRules.resolveReplacement(
                new BlockLayers(BlockStates.WATER, BlockStates.AIR), BlockLayer.PRIMARY, state
        );

        assertNotNull(layers);
        assertSame(BlockStates.WATER, layers.secondary());
    }

    @Test
    void hangsFromVerticalChainAndBreaksWhenItTurnsSideways() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Vector3i support = Direction.UP.relative(POSITION);
        world.setState(support, BlockStates.IRON_CHAIN.withTrait(BlockTraits.AXIS, Direction.Axis.Y));

        BlockState state = LanternBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(
                BlockStates.LANTERN, world.block(POSITION), null, Direction.DOWN, Vector3f.ZERO
        );

        assertNotNull(state);
        assertTrue(state.ensureTrait(BlockTraits.IS_HANGING));
        world.setState(POSITION, state);
        world.setState(support, BlockStates.IRON_CHAIN.withTrait(BlockTraits.AXIS, Direction.Axis.X));
        LanternBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(support));

        assertSame(BlockStates.AIR, world.state(POSITION));
        assertNull(LanternBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(
                BlockStates.LANTERN, world.block(POSITION), null, Direction.DOWN, Vector3f.ZERO
        ));
    }

    @Test
    void weatheringKeepsMountAndHonorsYoungerCopperAndBlockFormCancellation() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        BlockState exposed = BlockTypes.EXPOSED_COPPER_LANTERN.getDefaultState()
                .withTrait(BlockTraits.IS_HANGING, true);
        Vector3i adjacent = Direction.EAST.relative(POSITION);
        world.setState(POSITION, exposed);
        world.setState(adjacent, BlockTypes.COPPER_BLOCK.getDefaultState());
        Random random = new Random() {
            @Override
            public float nextFloat() {
                return 0;
            }
        };

        CopperWeatheringBlockHandlers.ON_RANDOM_TICK.execute(world.block(POSITION), random);
        assertSame(exposed, world.state(POSITION));

        world.setState(adjacent, BlockTypes.WAXED_COPPER.getDefaultState());
        CopperWeatheringBlockHandlers.ON_RANDOM_TICK.execute(world.block(POSITION), random);
        assertSame(BlockTypes.WEATHERED_COPPER_LANTERN, world.state(POSITION).getType());
        assertTrue(world.state(POSITION).ensureTrait(BlockTraits.IS_HANGING));

        world.onEvent(BlockFormEvent.class, event -> event.setCancelled(true));
        CopperWeatheringBlockHandlers.ON_RANDOM_TICK.execute(world.block(POSITION), random);
        assertSame(BlockTypes.WEATHERED_COPPER_LANTERN, world.state(POSITION).getType());
    }
}
