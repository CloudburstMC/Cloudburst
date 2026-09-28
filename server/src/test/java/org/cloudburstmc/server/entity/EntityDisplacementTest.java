package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityDisplacementTest {

    @Test
    void liftsEntitiesToTheNewSurfaceWithoutReplayingLandingPhysics() {
        DisplacedEntity entity = new DisplacedEntity();
        entity.setPosition(Vector3f.from(0.5f, 0.9375f, 0.5f));
        entity.resetFallDistance();
        entity.onGround = true;
        entity.motion = Vector3f.from(0.2f, 0, 0.1f);
        entity.stuckSpeedMultiplier = Vector3f.from(0.25f, 0.05f, 0.25f);

        VoxelShape addedShape = CloudVoxelShapes.onlySecond(CloudVoxelShapes.box(0, 0, 0, 1, 0.9375f, 1), CloudVoxelShapes.block());
        float lift = 1 + addedShape.collide(Direction.Axis.Y, entity.getBoundingBox().move(0, 1, 0), -1);
        assertEquals(0.0625f, lift);

        entity.displace(Vector3f.from(0, lift, 0));

        assertEquals(Vector3f.from(0.5f, 1, 0.5f), entity.getPosition());
        assertEquals(1, entity.getBoundingBox().getMinY());
        assertFalse(addedShape.overlaps(entity.getBoundingBox()));
        assertEquals(Vector3f.from(0.2f, 0, 0.1f), entity.getMotion());
        assertEquals(Vector3f.from(0.25f, 0.05f, 0.25f), entity.stuckSpeedMultiplier);
        assertTrue(entity.isOnGround());
        assertEquals(1, entity.highestPosition);
        assertEquals(0, entity.fallDistance);
        assertEquals(1, entity.synchronizations);
    }

    @Test
    void ignoresZeroDisplacementAndClosedEntities() {
        DisplacedEntity entity = new DisplacedEntity();
        Vector3f position = Vector3f.from(0, 1, 0);
        entity.setPosition(position);
        BoundingBox bounds = entity.getBoundingBox();
        entity.displace(Vector3f.ZERO);
        entity.closed = true;
        entity.displace(Vector3f.from(0, 1, 0));

        assertEquals(position, entity.getPosition());
        assertEquals(bounds, entity.getBoundingBox());
        assertEquals(0, entity.synchronizations);
    }

    private static class DisplacedEntity extends CloudEntity {
        private int synchronizations;

        private DisplacedEntity() {
            super(EntityTypes.PIG, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        }

        @Override
        public float getWidth() {
            return 0.6f;
        }

        @Override
        public float getHeight() {
            return 1.8f;
        }

        @Override
        protected void checkChunks() {
        }

        @Override
        protected void updateFallState(boolean onGround) {
            fail("Shape displacement must not invoke landing callbacks");
        }

        @Override
        public void sendAuthoritativeDisplacement() {
            this.synchronizations++;
        }
    }
}
