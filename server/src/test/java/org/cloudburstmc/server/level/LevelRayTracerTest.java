package org.cloudburstmc.server.level;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.BlockShapeMode;
import org.cloudburstmc.api.level.FluidCollisionMode;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.util.*;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LevelRayTracerTest {

    private static final Vector3f START = Vector3f.from(0.5f, 0.5f, 0.5f);
    private static final Vector3f END = Vector3f.from(3.5f, 0.5f, 0.5f);

    @Test
    void returnsExactBlockHitAndFace() {
        Block solid = block(CloudVoxelShapes.block(), CloudVoxelShapes.block(), LiquidState.empty());
        Block empty = block(CloudVoxelShapes.empty(), CloudVoxelShapes.empty(), LiquidState.empty());
        Level level = level(solid, empty, Set.of());

        BlockHitResult hit = assertInstanceOf(BlockHitResult.class,
                LevelRayTracer.rayTraceBlocks(level, context(BlockShapeMode.COLLIDER, FluidCollisionMode.NONE)));

        assertSame(solid, hit.block());
        assertEquals(Vector3f.from(2, 0.5f, 0.5f), hit.position());
        assertEquals(Direction.WEST, hit.face());
    }

    @Test
    void includesBlockFaceAtSegmentEnd() {
        Block solid = block(CloudVoxelShapes.block(), CloudVoxelShapes.block(), LiquidState.empty());
        Block empty = block(CloudVoxelShapes.empty(), CloudVoxelShapes.empty(), LiquidState.empty());

        BlockHitResult hit = assertInstanceOf(BlockHitResult.class,
                LevelRayTracer.rayTraceBlocks(level(solid, empty, Set.of()),
                        new RayTraceContext(START, Vector3f.from(2, 0.5f, 0.5f), BlockShapeMode.COLLIDER,
                                FluidCollisionMode.NONE, CollisionContext.empty())));

        assertSame(solid, hit.block());
        assertEquals(Direction.WEST, hit.face());
    }

    @Test
    void nearerEntityWinsOverBlock() {
        Block solid = block(CloudVoxelShapes.block(), CloudVoxelShapes.block(), LiquidState.empty());
        Block empty = block(CloudVoxelShapes.empty(), CloudVoxelShapes.empty(), LiquidState.empty());
        Entity entity = InterfaceProxy.create(Entity.class, Map.of("getBoundingBox",
                new BoundingBox(1.2f, 0, 0, 1.6f, 1, 1)));
        Level level = level(solid, empty, Set.of(entity));

        EntityHitResult hit = assertInstanceOf(EntityHitResult.class,
                LevelRayTracer.rayTrace(level, context(BlockShapeMode.COLLIDER, FluidCollisionMode.NONE),
                        0, candidate -> true));
        assertSame(entity, hit.entity());
    }

    @Test
    void blockOccludesEntityBehindIt() {
        Block solid = block(CloudVoxelShapes.block(), CloudVoxelShapes.block(), LiquidState.empty());
        Block empty = block(CloudVoxelShapes.empty(), CloudVoxelShapes.empty(), LiquidState.empty());
        Entity entity = InterfaceProxy.create(Entity.class, Map.of("getBoundingBox",
                new BoundingBox(2.5f, 0, 0, 2.9f, 1, 1)));
        Level level = level(solid, empty, Set.of(entity));

        BlockHitResult hit = assertInstanceOf(BlockHitResult.class,
                LevelRayTracer.rayTrace(level, context(BlockShapeMode.COLLIDER, FluidCollisionMode.NONE),
                        0, candidate -> true));
        assertSame(solid, hit.block());
    }

    @Test
    void outlineAndCollisionClipsUseDifferentShapes() {
        Block outlined = block(CloudVoxelShapes.empty(), CloudVoxelShapes.block(), LiquidState.empty());
        Block empty = block(CloudVoxelShapes.empty(), CloudVoxelShapes.empty(), LiquidState.empty());
        Level level = level(outlined, empty, Set.of());

        MissHitResult miss = assertInstanceOf(MissHitResult.class,
                LevelRayTracer.rayTraceBlocks(level, context(BlockShapeMode.COLLIDER, FluidCollisionMode.NONE)));
        assertEquals(MissReason.CLEAR, miss.reason());
        BlockHitResult hit = assertInstanceOf(BlockHitResult.class,
                LevelRayTracer.rayTraceBlocks(level, context(BlockShapeMode.OUTLINE, FluidCollisionMode.NONE)));
        assertSame(outlined, hit.block());
    }

    @Test
    void collisionRayTracePassesEntityContextToBlockShape() {
        Entity source = InterfaceProxy.create(Entity.class, Map.of(
                "getMotion", Vector3f.ZERO,
                "getBoundingBox", new BoundingBox(0, 0, 0, 1, 1, 1)));
        CollisionContext collision = CollisionContext.of(source);
        Block conditional = (Block) Proxy.newProxyInstance(Block.class.getClassLoader(),
                new Class<?>[]{Block.class}, (proxy, method, arguments) -> switch (method.getName()) {
                    case "getCollisionShape" -> arguments[0] == collision
                            ? CloudVoxelShapes.block() : CloudVoxelShapes.empty();
                    case "getLiquid" -> LiquidState.empty();
                    default -> throw new UnsupportedOperationException(method.toString());
                });
        Block empty = block(CloudVoxelShapes.empty(), CloudVoxelShapes.empty(), LiquidState.empty());

        BlockHitResult hit = assertInstanceOf(BlockHitResult.class,
                LevelRayTracer.rayTraceBlocks(level(conditional, empty, Set.of()),
                        new RayTraceContext(START, END, BlockShapeMode.COLLIDER, FluidCollisionMode.NONE, collision)));
        assertSame(conditional, hit.block());
    }

    @Test
    void fluidModesSelectSourceAndFlowingLiquid() {
        BlockType type = BlockType.of(Identifier.parse("test:clip_water"), BlockTraits.LIQUID_DEPTH);
        BlockRegistrationAccess.bindLiquidType(type, LiquidTypes.FLOWING_WATER);
        BlockState source = type.getDefaultState().withTrait(BlockTraits.LIQUID_DEPTH, 0);
        BlockState flowing = type.getDefaultState().withTrait(BlockTraits.LIQUID_DEPTH, 1);
        Block empty = block(CloudVoxelShapes.empty(), CloudVoxelShapes.empty(), LiquidState.empty());
        Block flowingBlock = block(CloudVoxelShapes.empty(), CloudVoxelShapes.empty(), LiquidState.of(flowing));
        Block sourceBlock = block(CloudVoxelShapes.empty(), CloudVoxelShapes.empty(), LiquidState.of(source));

        assertEquals(MissReason.CLEAR, assertInstanceOf(MissHitResult.class,
                LevelRayTracer.rayTraceBlocks(level(flowingBlock, empty, Set.of()),
                        context(BlockShapeMode.COLLIDER, FluidCollisionMode.SOURCE_ONLY))).reason());
        BlockHitResult flowingHit = assertInstanceOf(BlockHitResult.class,
                LevelRayTracer.rayTraceBlocks(level(flowingBlock, empty, Set.of()),
                        context(BlockShapeMode.COLLIDER, FluidCollisionMode.ANY)));
        assertSame(flowingBlock, flowingHit.block());
        assertTrue(flowingHit.liquid());
        assertSame(sourceBlock, assertInstanceOf(BlockHitResult.class,
                LevelRayTracer.rayTraceBlocks(level(sourceBlock, empty, Set.of()),
                        context(BlockShapeMode.COLLIDER, FluidCollisionMode.SOURCE_ONLY))).block());
        assertSame(flowingBlock, assertInstanceOf(BlockHitResult.class,
                LevelRayTracer.rayTraceBlocks(level(flowingBlock, empty, Set.of()),
                        context(BlockShapeMode.COLLIDER, FluidCollisionMode.WATER))).block());
    }

    @Test
    void unloadedCellsOccludeEntitiesBehindThem() {
        Block empty = block(CloudVoxelShapes.empty(), CloudVoxelShapes.empty(), LiquidState.empty());
        Entity entity = InterfaceProxy.create(Entity.class, Map.of("getBoundingBox",
                new BoundingBox(2.5f, 0, 0, 2.9f, 1, 1)));
        Level level = (Level) Proxy.newProxyInstance(Level.class.getClassLoader(), new Class<?>[]{Level.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getLoadedBlock" -> ((Vector3i) arguments[0]).getX() == 2 ? null : empty;
                    case "getNearbyEntities" -> Set.of(entity);
                    default -> throw new UnsupportedOperationException(method.toString());
                });

        MissHitResult miss = assertInstanceOf(MissHitResult.class,
                LevelRayTracer.rayTrace(level, context(BlockShapeMode.COLLIDER, FluidCollisionMode.NONE),
                        0, candidate -> true));
        assertEquals(MissReason.UNLOADED, miss.reason());
        assertEquals(Vector3f.from(2, 0.5f, 0.5f), miss.position());
        assertEquals(miss, LevelRayTracer.rayTraceBlocks(level,
                context(BlockShapeMode.COLLIDER, FluidCollisionMode.NONE)));
    }

    @Test
    void entityTraceReturnsMissAtRequestedEnd() {
        Block empty = block(CloudVoxelShapes.empty(), CloudVoxelShapes.empty(), LiquidState.empty());
        MissHitResult miss = assertInstanceOf(MissHitResult.class,
                LevelRayTracer.rayTraceEntities(level(empty, empty, Set.of()), START, END,
                        0, candidate -> true));
        assertEquals(MissReason.CLEAR, miss.reason());
        assertEquals(END, miss.position());
    }

    @Test
    void boxIntersectionDoesNotInventAWorldTarget() {
        BoxIntersection hit = new BoundingBox(2, 0, 0, 3, 1, 1).intersectSegment(START, END);
        assertNotNull(hit);
        assertEquals(Vector3f.from(2, 0.5f, 0.5f), hit.position());
        assertEquals(Direction.WEST, hit.face());
    }

    private static RayTraceContext context(BlockShapeMode blocks, FluidCollisionMode fluids) {
        return new RayTraceContext(START, END, blocks, fluids, CollisionContext.empty());
    }

    private static Block block(VoxelShape collision, VoxelShape outline, LiquidState liquid) {
        return InterfaceProxy.create(Block.class, Map.of("getCollisionShape", collision,
                "getOutlineShape", outline, "getLiquid", liquid));
    }

    private static Level level(Block solid, Block empty, Set<Entity> entities) {
        return (Level) Proxy.newProxyInstance(Level.class.getClassLoader(), new Class<?>[]{Level.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getLoadedBlock" -> ((Vector3i) arguments[0]).getX() == 2 ? solid : empty;
                    case "getNearbyEntities" -> entities;
                    default -> throw new UnsupportedOperationException(method.toString());
                });
    }
}
