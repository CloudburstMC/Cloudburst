package org.cloudburstmc.api.util.data;

/**
 * Visible cracking stages as an egg approaches hatching.
 */
public enum CrackedState {
    /**
     * An uncracked shell.
     */
    NO_CRACKS,
    /**
     * A partially cracked shell.
     */
    CRACKED,
    /**
     * The final cracked stage before hatching.
     */
    MAX_CRACKED
}
