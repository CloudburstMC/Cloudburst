package org.cloudburstmc.server.level.collision;

import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoxelShapeTest {

    @Test
    void unionCoverageAcceptsTiledBoxes() {
        VoxelShape covering = CloudVoxelShapes.fromBoxes(
                0, 0, 0, 0.5f, 1, 1,
                0.5f, 0, 0, 1, 1, 1
        );

        assertTrue(covering.covers(CloudVoxelShapes.block()));
    }

    @Test
    void fullBlockCheckRejectsShapeWithInternalHole() {
        VoxelShape hollow = CloudVoxelShapes.fromBoxes(
                0, 0, 0, 0.25f, 1, 1,
                0.75f, 0, 0, 1, 1, 1
        );

        assertFalse(CloudVoxelShapes.isFullBlock(hollow));
    }

    @Test
    void faceShapeUsesOnlyBoxesTouchingRequestedFace() {
        VoxelShape shape = CloudVoxelShapes.fromBoxes(
                0, 0, 0, 1, 0.5f, 1,
                0, 0.75f, 0, 1, 0.875f, 1
        );

        assertTrue(shape.getFaceShape(Direction.DOWN).covers(CloudVoxelShapes.block()));
        assertTrue(shape.getFaceShape(Direction.UP).isEmpty());
    }

    @Test
    void rejectsPathologicalBoxCounts() {
        float[] boxes = new float[(CloudVoxelShape.MAX_BOXES + 1) * 6];
        for (int i = 0; i < boxes.length; i += 6) {
            boxes[i + 3] = 1;
            boxes[i + 4] = 1;
            boxes[i + 5] = 1;
        }

        assertThrows(IllegalArgumentException.class, () -> CloudVoxelShapes.fromBoxes(boxes));
    }

    @Test
    void rejectsNonFiniteBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> CloudVoxelShapes.box(0, 0, 0, Float.NaN, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> CloudVoxelShapes.box(0, 0, 0, Float.POSITIVE_INFINITY, 1, 1));
    }
}
