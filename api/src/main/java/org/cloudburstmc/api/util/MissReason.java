package org.cloudburstmc.api.util;

/**
 * Why a level ray trace did not hit anything.
 */
public enum MissReason {
    /**
     * The whole requested segment was clear.
     */
    CLEAR,
    /**
     * The trace stopped at terrain that was not loaded.
     */
    UNLOADED
}
