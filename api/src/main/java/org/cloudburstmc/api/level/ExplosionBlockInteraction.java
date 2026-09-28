package org.cloudburstmc.api.level;

/**
 * Determines how an explosion affects blocks and their drops.
 */
public enum ExplosionBlockInteraction {
    /**
     * Leave blocks unchanged.
     */
    KEEP,
    /**
     * Destroy affected blocks and retain their normal drops.
     */
    DESTROY,
    /**
     * Destroy affected blocks, with individual drops surviving according to blast radius.
     */
    DESTROY_WITH_DECAY
}
