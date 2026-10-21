package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.LightOcclusionShapeHandler;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.data.StairShape;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class LightOcclusionBlockHandlers {

    public static final LightOcclusionShapeHandler OUTLINE = BlockState::getOutlineShape;
    public static final LightOcclusionShapeHandler STAIRS = LightOcclusionBlockHandlers::stairs;
    public static final LightOcclusionShapeHandler SHELF = LightOcclusionBlockHandlers::shelf;

    public static final LightOcclusionShapeHandler LECTERN = state -> CloudVoxelShapes.fromBoxes(
            0, 0, 0, 1, 2 / 16f, 1,
            4 / 16f, 2 / 16f, 4 / 16f, 12 / 16f, 14 / 16f, 12 / 16f
    );

    public static final LightOcclusionShapeHandler END_PORTAL_FRAME = state -> state.ensureTrait(BlockTraits.HAS_END_PORTAL_EYE)
            ? CloudVoxelShapes.fromBoxes(0, 0, 0, 1, 13 / 16f, 1, 4 / 16f, 13 / 16f, 4 / 16f, 12 / 16f, 1, 12 / 16f)
            : CloudVoxelShapes.box(0, 0, 0, 1, 13 / 16f, 1);

    private static VoxelShape stairs(BlockState state) {
        Direction facing = state.ensureTrait(BlockTraits.DIRECTION);
        StairShape shape = state.ensureTrait(BlockTraits.STAIR_SHAPE);
        boolean upsideDown = state.ensureTrait(BlockTraits.IS_UPSIDE_DOWN);

        float baseMinY = upsideDown ? 0.5f : 0;
        float baseMaxY = upsideDown ? 1 : 0.5f;
        float stepMinY = upsideDown ? 0 : 0.5f;
        float stepMaxY = upsideDown ? 0.5f : 1;

        List<Float> boxes = new ArrayList<>();
        addBox(boxes, 0, baseMinY, 0, 1, baseMaxY, 1);

        Direction side = switch (shape) {
            case INNER_LEFT, OUTER_LEFT -> facing.rotateCounterClockwise();
            default -> facing.rotateClockwise();
        };

        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                boolean forward = inHalf(facing, x, z);
                boolean lateral = inHalf(side, x, z);
                boolean occupied = switch (shape) {
                    case NONE -> forward;
                    case INNER_LEFT, INNER_RIGHT -> forward || lateral;
                    case OUTER_LEFT, OUTER_RIGHT -> forward && lateral;
                };

                if (occupied) {
                    addBox(boxes, x * 0.5f, stepMinY, z * 0.5f, (x + 1) * 0.5f, stepMaxY, (z + 1) * 0.5f);
                }
            }
        }

        return packedShape(boxes);
    }

    private static boolean inHalf(Direction direction, int x, int z) {
        return switch (direction) {
            case NORTH -> z == 0;
            case SOUTH -> z == 1;
            case WEST -> x == 0;
            case EAST -> x == 1;
            default -> throw new IllegalArgumentException("Stairs must face horizontally");
        };
    }

    private static VoxelShape shelf(BlockState state) {
        Direction facing = state.ensureTrait(BlockTraits.CARDINAL_DIRECTION).toDirection();
        VoxelShape north = CloudVoxelShapes.fromBoxes(
                0, 12 / 16f, 11 / 16f, 1, 1, 13 / 16f,
                0, 0, 13 / 16f, 1, 1, 1,
                0, 0, 11 / 16f, 1, 4 / 16f, 13 / 16f
        );

        List<Float> boxes = new ArrayList<>();
        north.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            switch (facing) {
                case NORTH -> addBox(boxes, minX, minY, minZ, maxX, maxY, maxZ);
                case SOUTH -> addBox(boxes, 1 - maxX, minY, 1 - maxZ, 1 - minX, maxY, 1 - minZ);
                case EAST -> addBox(boxes, 1 - maxZ, minY, minX, 1 - minZ, maxY, maxX);
                case WEST -> addBox(boxes, minZ, minY, 1 - maxX, maxZ, maxY, 1 - minX);
                default -> throw new IllegalArgumentException("Shelves must face horizontally");
            }
        });

        return packedShape(boxes);
    }

    private static void addBox(List<Float> boxes, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        boxes.add(minX);
        boxes.add(minY);
        boxes.add(minZ);
        boxes.add(maxX);
        boxes.add(maxY);
        boxes.add(maxZ);
    }

    private static VoxelShape packedShape(List<Float> boxes) {
        float[] packed = new float[boxes.size()];
        for (int i = 0; i < packed.length; i++) {
            packed[i] = boxes.get(i);
        }

        return CloudVoxelShapes.fromBoxes(packed);
    }
}
