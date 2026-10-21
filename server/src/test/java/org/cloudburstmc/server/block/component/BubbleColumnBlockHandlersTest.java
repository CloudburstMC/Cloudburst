package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityInsideBlockEvent;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.EntityInsideBlockScanner;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BubbleColumnBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void schedulesFormationSeparatelyFromLiquidSimulation() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION.add(0, -1, 0), BlockTypes.SOUL_SAND.getDefaultState());
        world.setState(POSITION, BlockStates.WATER);

        REGISTRY.requireComponent(BlockTypes.WATER, BlockComponents.ON_STATE_CHANGED).execute(world.block(POSITION));

        assertEquals(List.of(20), world.getScheduledDelays());
        assertSame(BlockStates.WATER, world.state(POSITION));
        REGISTRY.requireComponent(BlockTypes.WATER, BlockComponents.ON_TICK).execute(world.block(POSITION), null);

        assertSame(BlockTypes.BUBBLE_COLUMN, world.state(POSITION).getType());
    }

    @Test
    void formsSourceWaterColumnsAndReversesTheWholeColumn() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        Vector3i support = POSITION.add(0, -1, 0);
        world.setState(support, BlockTypes.SOUL_SAND.getDefaultState());
        for (int y = 0; y < 4; y++) {
            world.setState(POSITION.add(0, y, 0), BlockStates.WATER);
        }

        BubbleColumnBlockHandlers.update(world.block(POSITION));
        for (int y = 0; y < 4; y++) {
            Block block = world.block(POSITION.add(0, y, 0));

            assertSame(BlockTypes.BUBBLE_COLUMN, block.getState().getType());
            assertFalse(block.getState().ensureTrait(BlockTraits.HAS_DRAG_DOWN));
            assertTrue(block.getLiquid().isSource());
        }

        world.setState(support, BlockTypes.MAGMA.getDefaultState());
        BubbleColumnBlockHandlers.update(world.block(POSITION));

        for (int y = 0; y < 4; y++) {
            assertTrue(world.state(POSITION.add(0, y, 0)).ensureTrait(BlockTraits.HAS_DRAG_DOWN));
        }
    }

    @Test
    void neverReplacesWaterloggedBlocksPlantsOrFlowingWater() {
        for (BlockState obstruction : List.of(BlockTypes.KELP.getDefaultState(), BlockTypes.SEAGRASS.getDefaultState(),
                BlockTypes.OAK_SLAB.getDefaultState(), BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 1))) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            world.setState(POSITION.add(0, -1, 0), BlockTypes.SOUL_SAND.getDefaultState());
            world.setState(POSITION, BlockStates.WATER);
            world.setState(POSITION.add(0, 1, 0), obstruction);
            if (!obstruction.getType().isLiquid()) {
                world.setLiquid(POSITION.add(0, 1, 0), LiquidState.of(BlockStates.WATER));
            }
            world.setState(POSITION.add(0, 2, 0), BlockStates.WATER);

            BubbleColumnBlockHandlers.update(world.block(POSITION));

            assertSame(obstruction, world.state(POSITION.add(0, 1, 0)));
            assertSame(BlockStates.WATER, world.state(POSITION.add(0, 2, 0)));
        }
    }

    @Test
    void losingSupportRestoresWaterAndPropagationStopsAtUnloadedBlocks() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        BlockState bubble = BlockTypes.BUBBLE_COLUMN.getDefaultState();
        world.setState(POSITION, bubble);
        world.setState(POSITION.add(0, 1, 0), bubble);

        BubbleColumnBlockHandlers.update(world.block(POSITION));

        assertSame(BlockStates.WATER, world.state(POSITION));
        assertSame(BlockStates.WATER, world.state(POSITION.add(0, 1, 0)));

        world.setState(POSITION.add(0, -1, 0), BlockTypes.SOUL_SAND.getDefaultState());
        world.unload(POSITION.add(0, 1, 0));
        BubbleColumnBlockHandlers.update(world.block(POSITION));

        assertSame(BlockTypes.BUBBLE_COLUMN, world.state(POSITION).getType());
        assertSame(BlockStates.WATER, world.state(POSITION.add(0, 1, 0)));
    }

    @Test
    void columnMotionUsesVanillaLimitsAndInsideEffectsCanBeCancelled() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        BlockState column = BlockTypes.BUBBLE_COLUMN.getDefaultState().withTrait(BlockTraits.HAS_DRAG_DOWN, false);
        world.setState(POSITION, column);
        world.setState(POSITION.add(0, 1, 0), BlockStates.WATER);
        AtomicReference<Vector3f> motion = new AtomicReference<>(Vector3f.from(0.2f, 0.69f, -0.3f));
        AtomicInteger fallResets = new AtomicInteger();
        Entity entity = InterfaceProxy.create(Entity.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "getMotion" -> motion.get();
            case "getBoundingBox" -> new BoundingBox(0, 64, 0, 0.6f, 65.8f, 0.6f);
            case "setMotion" -> {
                motion.set((Vector3f) arguments[0]);
                yield true;
            }
            case "resetFallDistance" -> {
                fallResets.incrementAndGet();
                yield null;
            }
            default -> throw new AssertionError("Unexpected entity access: " + method);
        });

        EntityInsideBlockScanner.applyEffects(world.block(POSITION), entity, true);

        assertEquals(Vector3f.from(0.2f, 0.7f, -0.3f), motion.get());
        assertEquals(1, fallResets.get());

        world.setState(POSITION.add(0, 1, 0), BlockStates.AIR);
        motion.set(Vector3f.from(0.2f, 1.75f, -0.3f));
        EntityInsideBlockScanner.applyEffects(world.block(POSITION), entity, true);

        assertEquals(Vector3f.from(0.2f, 1.8f, -0.3f), motion.get());
        assertEquals(1, fallResets.get());

        world.setState(POSITION, column.withTrait(BlockTraits.HAS_DRAG_DOWN, true));
        world.setState(POSITION.add(0, 1, 0), BlockStates.WATER);
        motion.set(Vector3f.from(0.2f, -0.29f, -0.3f));
        EntityInsideBlockScanner.applyEffects(world.block(POSITION), entity, true);

        assertEquals(Vector3f.from(0.2f, -0.3f, -0.3f), motion.get());
        assertEquals(2, fallResets.get());

        world.setState(POSITION.add(0, 1, 0), BlockStates.AIR);
        motion.set(Vector3f.from(0.2f, -0.89f, -0.3f));
        EntityInsideBlockScanner.applyEffects(world.block(POSITION), entity, true);

        assertEquals(Vector3f.from(0.2f, -0.9f, -0.3f), motion.get());
        assertEquals(2, fallResets.get());

        world.onEvent(EntityInsideBlockEvent.class, event -> event.setCancelled(true));
        motion.set(Vector3f.ZERO);
        EntityInsideBlockScanner.applyEffects(world.block(POSITION), entity, true);

        assertEquals(Vector3f.ZERO, motion.get());
        assertEquals(2, fallResets.get());
    }
}
