package org.cloudburstmc.server.level.light;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.component.LightOcclusionShapeHandler;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Answers whether light passes between two neighbouring cells.
 *
 * <p>Light is blocked when the face shapes of both layers of both cells
 * together cover the shared face; complementary partial faces seal it together.
 * Face shapes are cached per state.
 */
@UtilityClass
public class LightOcclusion {

    private static final ConcurrentMap<BlockState, Faces> CACHE = new ConcurrentHashMap<>();

    public static boolean occludes(BlockRegistry registry, BlockState fromPrimary, BlockState fromSecondary,
            BlockState toPrimary, BlockState toSecondary, Direction direction) {
        VoxelShape remaining = CloudVoxelShapes.block();
        remaining = subtractFace(registry, fromPrimary, direction, remaining);
        remaining = subtractFace(registry, fromSecondary, direction, remaining);
        remaining = subtractFace(registry, toPrimary, direction.getOpposite(), remaining);
        remaining = subtractFace(registry, toSecondary, direction.getOpposite(), remaining);
        return remaining.isEmpty();
    }

    /**
     * Returns the face shape of a block state towards a neighbour.
     *
     * @param registry  the block registry
     * @param state     the block state
     * @param direction the direction towards the neighbour
     * @return the face shape in that direction
     */
    public static VoxelShape faceShape(BlockRegistry registry, BlockState state, Direction direction) {
        return faces(registry, state).shapes()[direction.ordinal()];
    }

    public static boolean affectsLight(BlockRegistry registry, BlockState before, BlockState after) {
        if (before == after) {
            return false;
        }

        if (before.getLightDampening() != after.getLightDampening() || before.getLightEmission() != after.getLightEmission()) {
            return true;
        }

        Faces oldFaces = faces(registry, before);
        Faces newFaces = faces(registry, after);
        for (Direction direction : Direction.values()) {
            VoxelShape oldFace = oldFaces.shapes()[direction.ordinal()];
            VoxelShape newFace = newFaces.shapes()[direction.ordinal()];
            if (oldFace != newFace && (!oldFace.covers(newFace) || !newFace.covers(oldFace))) {
                return true;
            }
        }

        return false;
    }

    private static VoxelShape subtractFace(BlockRegistry registry, BlockState state, Direction direction, VoxelShape remaining) {
        if (remaining.isEmpty()) {
            return remaining;
        }

        VoxelShape face = faces(registry, state).shapes()[direction.ordinal()];
        return face.isEmpty() ? remaining : CloudVoxelShapes.onlySecond(face, remaining);
    }

    private static Faces faces(BlockRegistry registry, BlockState state) {
        LightOcclusionShapeHandler handler = registry.requireComponent(state.getType(), BlockComponents.GET_LIGHT_OCCLUSION_SHAPE);
        Faces cached = CACHE.get(state);
        if (cached != null && cached.handler() == handler) {
            return cached;
        }

        Faces created = createFaces(state, handler);
        CACHE.put(state, created);
        return created;
    }

    private static Faces createFaces(BlockState state, LightOcclusionShapeHandler handler) {
        VoxelShape shape = handler.execute(state);
        VoxelShape[] faces = new VoxelShape[Direction.values().length];
        for (Direction direction : Direction.values()) {
            faces[direction.ordinal()] = shape.getFaceShape(direction);
        }
        return new Faces(handler, faces);
    }

    private record Faces(LightOcclusionShapeHandler handler, VoxelShape[] shapes) {
    }
}
