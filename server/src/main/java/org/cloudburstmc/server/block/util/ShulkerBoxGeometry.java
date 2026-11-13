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

    public static BoundingBox sweptLid(Vector3i position, Direction facing, float from, float to) {
        float min = Math.min(from, to);
        float max = Math.max(from, to);

        float x = position.getX();
        float y = position.getY();
        float z = position.getZ();

        return switch (facing) {
            case DOWN -> new BoundingBox(x, y - max, z, x + 1, y - min, z + 1);
            case UP -> new BoundingBox(x, y + 1 + min, z, x + 1, y + 1 + max, z + 1);
            case NORTH -> new BoundingBox(x, y, z - max, x + 1, y + 1, z - min);
            case SOUTH -> new BoundingBox(x, y, z + 1 + min, x + 1, y + 1, z + 1 + max);
            case WEST -> new BoundingBox(x - max, y, z, x - min, y + 1, z + 1);
            case EAST -> new BoundingBox(x + 1 + min, y, z, x + 1 + max, y + 1, z + 1);
        };
    }

    private static float extension(float progress) {
        return Math.clamp(progress, 0, 1) * 0.5f;
    }
}
