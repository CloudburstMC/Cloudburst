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
    void collisionUpdateReplacesPreviousContact() {
        DisplacedEntity entity = new DisplacedEntity();
        entity.updateCollisionContact(true, false, null);

        assertTrue(entity.hasHorizontalCollision());
        assertFalse(entity.isOnGround());

        entity.updateCollisionContact(false, true, null);
        assertFalse(entity.hasHorizontalCollision());
        assertTrue(entity.isOnGround());

        entity.updateCollisionContact(false, false, null);
        assertFalse(entity.isOnGround());
    }

    @Test
    void publishingMovementDoesNotOverwriteSimulationHistory() {
        DisplacedEntity entity = new DisplacedEntity();
        Vector3f previous = Vector3f.from(0, 1, 0);
        entity.lastPosition = previous;
        entity.setPosition(Vector3f.from(2, 1, 0));

        entity.updateMovement();
        entity.updateMovement();

        assertEquals(previous, entity.lastPosition);
        assertEquals(1, entity.movementUpdates);
    }

    @Test
    void liftsEntitiesToTheNewSurfaceWithoutReplayingLandingPhysics() {
        DisplacedEntity entity = new DisplacedEntity();
        entity.setPosition(Vector3f.from(0.5f, 0.9375f, 0.5f));
        entity.resetFallDistance();
        entity.setOnGround(true);
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
        assertEquals(0, entity.getFallDistance());
        assertEquals(1, entity.synchronizations);
    }

    @Test
    void closedEntityDoesNotRunItsTickHook() {
        DisplacedEntity entity = new DisplacedEntity();
        entity.close();

        assertFalse(entity.tick(1));
        assertEquals(0, entity.ticks);
    }

    @Test
    void tickHookRunsOncePerTickAndReceivesElapsedTime() {
        DisplacedEntity entity = new DisplacedEntity();

        assertTrue(entity.tick(1));
        assertTrue(entity.tick(1));
        assertEquals(1, entity.ticks);

        assertTrue(entity.tick(4));
        assertEquals(2, entity.ticks);
        assertEquals(3, entity.elapsedTicks);
    }

    @Test
    void horizontalLookVectorFollowsYaw() {
        DisplacedEntity entity = new DisplacedEntity();
        entity.setRotation(0, 45);
        assertEquals(0f, entity.getHorizontalLookVector().getX(), 0.00001f);
        assertEquals(1f, entity.getHorizontalLookVector().getY(), 0.00001f);

        entity.setRotation(90, -45);
        assertEquals(-1f, entity.getHorizontalLookVector().getX(), 0.00001f);
        assertEquals(0f, entity.getHorizontalLookVector().getY(), 0.00001f);
    }

    @Test
    void removedOwnerIsNotReturnedFromCache() {
        DisplacedEntity entity = new DisplacedEntity();
        DisplacedEntity owner = new DisplacedEntity();
        entity.setOwner(owner);
        assertSame(owner, entity.getOwner());

        owner.close();

        assertNull(entity.getOwner());
    }

    @Test
    void uninitializedEntityCannotSpawn() {
        DisplacedEntity entity = new DisplacedEntity();

        assertFalse(entity.spawn());
        assertFalse(entity.isClosed());
        assertFalse(entity.isValid());
        assertThrows(IllegalStateException.class, entity::restoreFromStorage);
    }

    @Test
    void invalidScaleLeavesBoundsUnchanged() {
        DisplacedEntity entity = new DisplacedEntity();
        entity.setScale(2f);
        BoundingBox bounds = entity.getBoundingBox();

        for (float scale : new float[]{Float.NaN, Float.POSITIVE_INFINITY, -1f, 0f, 0.01f, 17f}) {
            assertThrows(IllegalArgumentException.class, () -> entity.setScale(scale));
        }

        assertEquals(2f, entity.getScale());
        assertEquals(bounds, entity.getBoundingBox());
        entity.setScale(0.0625f);
        entity.setScale(16f);
        assertEquals(16f, entity.getScale());
    }

    @Test
    void invalidTransformDoesNotChangePositionOrRotation() {
        DisplacedEntity entity = new DisplacedEntity();
        Vector3f original = Vector3f.from(0, 1, 0);
        assertTrue(entity.setPositionAndRotation(original, 45, 15));

        assertThrows(IllegalArgumentException.class,
                () -> entity.setPosition(Vector3f.from(Float.NaN, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> entity.setPositionAndRotation(Vector3f.ZERO, 90, Float.POSITIVE_INFINITY));

        assertEquals(original, entity.getPosition());
        assertEquals(45f, entity.getYaw());
        assertEquals(15f, entity.getPitch());
    }

    @Test
    void closedEntityRejectsPositionAndRotationChange() {
        DisplacedEntity entity = new DisplacedEntity();
        Vector3f original = Vector3f.from(0, 1, 0);
        assertTrue(entity.setPositionAndRotation(original, 45, 15));
        entity.close();

        assertFalse(entity.setPositionAndRotation(Vector3f.ZERO, 90, 0));
        assertEquals(original, entity.getPosition());
        assertEquals(45f, entity.getYaw());
        assertEquals(15f, entity.getPitch());
    }

    @Test
    void fallDistanceRejectsInvalidValues() {
        DisplacedEntity entity = new DisplacedEntity();
        entity.setPosition(Vector3f.from(0, 5, 0));
        entity.setFallDistance(3);

        assertThrows(IllegalArgumentException.class, () -> entity.setFallDistance(-1));
        assertThrows(IllegalArgumentException.class, () -> entity.setFallDistance(Float.NaN));
        assertEquals(3, entity.getFallDistance());
        entity.resetFallDistance();
        assertEquals(0, entity.getFallDistance());
    }

    @Test
    void ignoresZeroDisplacementAndClosedEntities() {
        DisplacedEntity entity = new DisplacedEntity();
        Vector3f position = Vector3f.from(0, 1, 0);
        entity.setPosition(position);
        BoundingBox bounds = entity.getBoundingBox();
        entity.displace(Vector3f.ZERO);
        entity.close();
        entity.displace(Vector3f.from(0, 1, 0));

        assertEquals(position, entity.getPosition());
        assertEquals(bounds, entity.getBoundingBox());
        assertEquals(0, entity.synchronizations);
    }

    private static class DisplacedEntity extends CloudEntity {
        private int synchronizations;
        private int movementUpdates;
        private int ticks;
        private int elapsedTicks;

        private DisplacedEntity() {
            super(EntityTypes.PIG, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        }

        @Override
        public float getWidth() {
            return 0.6f;
        }

        @Override
        protected boolean tickEntity(int currentTick, int elapsedTicks) {
            this.ticks++;
            this.elapsedTicks = elapsedTicks;
            return true;
        }

        @Override
        public float getHeight() {
            return 1.8f;
        }

        @Override
        public float getBaseOffset() {
            return 0;
        }

        @Override
        public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
            this.movementUpdates++;
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
