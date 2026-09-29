package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Pose;

import java.util.Objects;

/**
 * Called after an entity's pose changes and its collision bounds are updated.
 * {@link Entity#getPose()} returns the new pose during this event.
 * Different poses may have the same dimensions. This notification cannot be canceled.
 */
public class EntityPoseChangeEvent extends EntityEvent {
    private final Pose previousPose;
    private final Pose pose;

    /**
     * @param entity       affected entity
     * @param previousPose pose before the transition
     * @param pose         new pose
     */
    public EntityPoseChangeEvent(Entity entity, Pose previousPose, Pose pose) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.previousPose = Objects.requireNonNull(previousPose, "previousPose");
        this.pose = Objects.requireNonNull(pose, "pose");
    }

    /**
     * @return pose before the transition
     */
    public Pose getPreviousPose() {
        return this.previousPose;
    }

    /**
     * @return new pose
     */
    public Pose getPose() {
        return this.pose;
    }
}
