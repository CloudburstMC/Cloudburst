package org.cloudburstmc.server.block.util;

import org.cloudburstmc.api.block.SupportType;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockSupportTest {

    private static final float PIXEL = 1f / 16f;

    @Test
    void fullSupportUsesUnionOfShapeBoxes() {
        VoxelShape tiledFace = CloudVoxelShapes.fromBoxes(
                0, 0, 0, 0.5f, 1, 1,
                0.5f, 0, 0, 1, 1, 1
        );

        assertTrue(BlockSupport.hasRequiredSupport(tiledFace, Direction.UP, SupportType.FULL));
    }

    @Test
    void centerSupportRequiresOnlyCenterPixels() {
        VoxelShape center = CloudVoxelShapes.box(7 * PIXEL, 0, 7 * PIXEL, 9 * PIXEL, 1, 9 * PIXEL);

        assertTrue(BlockSupport.hasRequiredSupport(center, Direction.UP, SupportType.CENTER));
        assertFalse(BlockSupport.hasRequiredSupport(center, Direction.UP, SupportType.FULL));
        assertFalse(BlockSupport.hasRequiredSupport(center, Direction.UP, SupportType.RIGID));
    }

    @Test
    void sideCenterSupportUsesVanillaFixedMask() {
        VoxelShape vanillaCenter = CloudVoxelShapes.box(0, 0, 7 * PIXEL, PIXEL, 10 * PIXEL, 9 * PIXEL);
        VoxelShape geometricallyCentered = CloudVoxelShapes.box(
                0, 7 * PIXEL, 7 * PIXEL, PIXEL, 9 * PIXEL, 9 * PIXEL
        );

        assertTrue(BlockSupport.hasRequiredSupport(vanillaCenter, Direction.WEST, SupportType.CENTER));
        assertFalse(BlockSupport.hasRequiredSupport(geometricallyCentered, Direction.WEST, SupportType.CENTER));
    }

    @Test
    void rigidSupportRequiresOuterRingInsteadOfCenter() {
        VoxelShape ring = CloudVoxelShapes.fromBoxes(
                0, 0, 0, 1, 1, 2 * PIXEL,
                0, 0, 14 * PIXEL, 1, 1, 1,
                0, 0, 2 * PIXEL, 2 * PIXEL, 1, 14 * PIXEL,
                14 * PIXEL, 0, 2 * PIXEL, 1, 1, 14 * PIXEL
        );

        assertTrue(BlockSupport.hasRequiredSupport(ring, Direction.UP, SupportType.RIGID));
        assertFalse(BlockSupport.hasRequiredSupport(ring, Direction.UP, SupportType.CENTER));
        assertFalse(BlockSupport.hasRequiredSupport(ring, Direction.UP, SupportType.FULL));
    }
}
