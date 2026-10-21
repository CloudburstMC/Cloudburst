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

@UtilityClass
public class LightOcclusion {

    private static final ConcurrentMap<BlockState, Faces> CACHE = new ConcurrentHashMap<>();

    public static boolean occludes(BlockRegistry registry, LightAccess.Sample from, LightAccess.Sample to, Direction direction) {
        VoxelShape remaining = CloudVoxelShapes.block();
        remaining = subtractFace(registry, from.primary(), direction, remaining);
        remaining = subtractFace(registry, from.secondary(), direction, remaining);
        remaining = subtractFace(registry, to.primary(), direction.getOpposite(), remaining);
        remaining = subtractFace(registry, to.secondary(), direction.getOpposite(), remaining);
        return remaining.isEmpty();
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
