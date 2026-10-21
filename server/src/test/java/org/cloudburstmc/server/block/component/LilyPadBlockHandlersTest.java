package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.vehicle.Boat;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.event.entity.EntityInsideBlockEvent;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.EntityInsideBlockScanner;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LilyPadBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void requiresSourceWaterOrAnIceSupportAndNoWaterAtThePad() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockTypes.WATERLILY.getDefaultState());
        Vector3i below = POSITION.add(0, -1, 0);

        for (BlockState support : List.of(BlockStates.WATER, BlockTypes.ICE.getDefaultState(), BlockTypes.FROSTED_ICE.getDefaultState())) {
            world.setState(below, support);
            assertTrue(LilyPadBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
        }

        for (BlockState support : List.of(BlockStates.AIR, BlockStates.STONE, BlockStates.LAVA,
                BlockTypes.PACKED_ICE.getDefaultState(), BlockTypes.BLUE_ICE.getDefaultState(),
                BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 1))) {
            world.setState(below, support);
            assertFalse(LilyPadBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
        }

        world.setState(below, BlockStates.WATER);
        world.setLiquid(POSITION, LiquidState.of(BlockStates.WATER));

        assertFalse(LilyPadBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
    }

    @Test
    void losingSupportBreaksThePad() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockTypes.WATERLILY.getDefaultState());

        REGISTRY.requireComponent(BlockTypes.WATERLILY, BlockComponents.ON_NEIGHBOUR_CHANGED)
                .execute(world.block(POSITION), world.block(POSITION.add(0, -1, 0)));

        assertSame(BlockStates.AIR, world.state(POSITION));
        assertEquals(1, world.getBreakCount());
    }

    @Test
    void boatDestructionCanBeCancelledAndDoesNotApplyToOtherEntities() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockTypes.WATERLILY.getDefaultState());
        Boat boat = InterfaceProxy.create(Boat.class, Map.of(
                "getMotion", Vector3f.ZERO,
                "getBoundingBox", new BoundingBox(0, 64, 0, 1.4f, 64.455f, 1.4f)
        ));
        Entity other = InterfaceProxy.create(Entity.class, Map.of());

        assertTrue(REGISTRY.requireComponent(BlockTypes.WATERLILY, BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE)
                .execute(BlockTypes.WATERLILY.getDefaultState(), CollisionContext.of(boat)).covers(CloudVoxelShapes.block()));

        EntityInsideBlockScanner.applyEffects(world.block(POSITION), other, true);

        assertTrue(world.getEvents(EntityChangeBlockEvent.class).isEmpty());
        assertEquals(0, world.getBreakCount());

        world.onEvent(EntityChangeBlockEvent.class, event -> event.setCancelled(true));
        EntityInsideBlockScanner.applyEffects(world.block(POSITION), boat, true);

        assertEquals(0, world.getBreakCount());
        assertSame(BlockTypes.WATERLILY, world.state(POSITION).getType());

        world.onEvent(EntityChangeBlockEvent.class, event -> event.setCancelled(false));
        EntityInsideBlockScanner.applyEffects(world.block(POSITION), boat, true);

        assertEquals(1, world.getBreakCount());
        assertSame(BlockStates.AIR, world.state(POSITION));
    }

    @Test
    void cancellingInsideEffectsPreventsTheBoatBlockChangeEvent() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockTypes.WATERLILY.getDefaultState());
        world.onEvent(EntityInsideBlockEvent.class, event -> event.setCancelled(true));
        Boat boat = InterfaceProxy.create(Boat.class, Map.of());

        EntityInsideBlockScanner.applyEffects(world.block(POSITION), boat, true);

        assertTrue(world.getEvents(EntityChangeBlockEvent.class).isEmpty());
        assertEquals(0, world.getBreakCount());
    }
}
