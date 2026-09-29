package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.Pose;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HumanPosesTest {

    @Test
    void usesLowDimensionsForSwimmingGlidingAndSpinAttacks() {
        EntityDimensions swimming = HumanPoses.dimensions(Pose.SWIMMING);
        assertEquals(new EntityDimensions(0.6f, 0.6f, 0.4f), swimming);
        assertEquals(swimming, HumanPoses.dimensions(Pose.FALL_FLYING));
        assertEquals(swimming, HumanPoses.dimensions(Pose.SPIN_ATTACK));
        assertEquals(0.625f, HumanPoses.dimensions(Pose.CRAWLING).height());
        assertEquals(1.27f, HumanPoses.dimensions(Pose.CROUCHING).eyeHeight());
    }

    @Test
    void fallsBackWithoutExpandingIntoOverheadBlocks() {
        assertEquals(Pose.CROUCHING, resolveWithHeadroom(Pose.SWIMMING, 1.6f));
        assertEquals(Pose.CRAWLING, resolveWithHeadroom(Pose.SWIMMING, 1.0f));
        assertEquals(Pose.SWIMMING, resolveWithHeadroom(Pose.SWIMMING, 0.61f));
        assertEquals(Pose.SWIMMING, resolveWithHeadroom(Pose.SWIMMING, 0.5f));
        assertEquals(Pose.STANDING, resolveWithHeadroom(Pose.CRAWLING, 2.0f));
    }

    @Test
    void unrestrictedPoseDoesNotCheckCollisions() {
        assertEquals(Pose.FALL_FLYING,
                HumanPoses.resolve(Pose.STANDING, Pose.FALL_FLYING, true, pose -> {
                    throw new AssertionError("Unexpected collision query");
                }));
    }

    @Test
    void scalesCollisionBoundsAroundTheFeet() {
        BoundingBox bounds = HumanPoses.dimensions(Pose.SWIMMING).boundingBox(Vector3f.from(2, 4, 6), 2);
        assertEquals(4.0f, bounds.getMinY());
        assertEquals(5.2f, bounds.getMaxY());
        assertEquals(1.4f, bounds.getMinX());
        assertEquals(2.6f, bounds.getMaxX());
    }

    private static Pose resolveWithHeadroom(Pose current, float headroom) {
        return HumanPoses.resolve(current, Pose.STANDING, false, pose -> HumanPoses.dimensions(pose).height() <= headroom);
    }
}
