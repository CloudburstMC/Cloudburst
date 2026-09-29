package org.cloudburstmc.api.entity;

/**
 * An entity's resolved posture. Movement requests such as sneaking or swimming
 * may differ from the pose when surrounding blocks prevent a transition.
 */
public enum Pose {
    /**
     * Upright posture.
     */
    STANDING,
    /**
     * Lowered upright posture used for sneaking or limited headroom.
     */
    CROUCHING,
    /**
     * Low horizontal posture used for swimming.
     */
    SWIMMING,
    /**
     * Low posture used to move through confined spaces on land.
     */
    CRAWLING,
    /**
     * Horizontal flight posture used while gliding with an elytra.
     */
    FALL_FLYING,
    /**
     * Spinning posture used during a Riptide attack.
     */
    SPIN_ATTACK,
    /**
     * Resting posture used while sleeping.
     */
    SLEEPING
}
