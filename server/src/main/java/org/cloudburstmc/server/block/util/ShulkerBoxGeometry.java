package org.cloudburstmc.server.block.util;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

@UtilityClass
public class ShulkerBoxGeometry {

    public static VoxelShape shape(Direction facing, float progress) {
        float extension = extension(progress);
        return switch (facing) {
            case DOWN -> CloudVoxelShapes.box(0, -extension, 0, 1, 1, 1);
            case UP -> CloudVoxelShapes.box(0, 0, 0, 1, 1 + extension, 1);
            case NORTH -> CloudVoxelShapes.box(0, 0, -extension, 1, 1, 1);
            case SOUTH -> CloudVoxelShapes.box(0, 0, 0, 1, 1, 1 + extension);
            case WEST -> CloudVoxelShapes.box(-extension, 0, 0, 1, 1, 1);
            case EAST -> CloudVoxelShapes.box(0, 0, 0, 1 + extension, 1, 1);
        };
    }

    public static BoundingBox box(Vector3i position, Direction facing, float progress) {
        return shape(facing, progress).bounds().move(position);
    }

    private static float extension(float progress) {
        return Math.clamp(progress, 0, 1) * 0.5f;
    }
}
