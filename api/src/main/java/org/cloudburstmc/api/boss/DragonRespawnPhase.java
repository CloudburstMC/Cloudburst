package org.cloudburstmc.api.boss;

/**
 * A phase of the Ender Dragon respawn sequence.
 */
public enum DragonRespawnPhase {
    /**
     * No respawn is in progress.
     */
    NONE,
    /**
     * The ritual crystals begin directing their beams into the sky.
     */
    START,
    /**
     * The ritual prepares to rebuild the End spikes.
     */
    PREPARING_TO_SUMMON_PILLARS,
    /**
     * The End spikes and their crystals are being rebuilt.
     */
    SUMMONING_PILLARS,
    /**
     * The ritual crystals are summoning the dragon.
     */
    SUMMONING_DRAGON,
    /**
     * The dragon is being added to the level and the ritual is ending.
     */
    END
}
