package org.cloudburstmc.server.entity;

import org.cloudburstmc.math.vector.Vector3f;

/**
 * Tracks the movement last published to viewers.
 */
public class EntityMovementTracker {

    private static final float POSITION_THRESHOLD_SQUARED = 0.0001f;
    private static final float ROTATION_THRESHOLD_SQUARED = 1.0f;
    private static final float VELOCITY_THRESHOLD_SQUARED = 0.0025f;

    private Vector3f position = Vector3f.ZERO;
    private Vector3f velocity = Vector3f.ZERO;
    private float yaw;
    private float pitch;

    public void resetTransform(Vector3f position, float yaw, float pitch) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void publishChanges(CloudEntity entity) {
        Vector3f position = entity.getPosition();
        float yaw = entity.getYaw();
        float pitch = entity.getPitch();
        float yawDelta = (float) Math.IEEEremainder(yaw - this.yaw, 360.0);
        float pitchDelta = pitch - this.pitch;

        if (position.distanceSquared(this.position) > POSITION_THRESHOLD_SQUARED || yawDelta * yawDelta + pitchDelta * pitchDelta > ROTATION_THRESHOLD_SQUARED) {
            this.resetTransform(position, yaw, pitch);
            entity.addMovement(position.getX(), position.getY() + entity.getBaseOffset(), position.getZ(), yaw, pitch, yaw);
        }

        Vector3f velocity = entity.getMotion();
        float velocityDelta = velocity.distanceSquared(this.velocity);
        boolean stopped = velocityDelta > POSITION_THRESHOLD_SQUARED && velocity.lengthSquared() <= POSITION_THRESHOLD_SQUARED;
        if (velocityDelta > VELOCITY_THRESHOLD_SQUARED || stopped) {
            this.velocity = velocity;
            entity.addMotion(velocity);
        }
    }
}
