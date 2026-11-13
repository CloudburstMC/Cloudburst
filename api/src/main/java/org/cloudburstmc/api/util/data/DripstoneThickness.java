package org.cloudburstmc.api.util.data;

/**
 * The shape of a segment within a pointed formation.
 */
public enum DripstoneThickness {
    /**
     * The exposed end of a formation.
     */
    TIP,
    /**
     * The tapering segment immediately behind the tip.
     */
    FRUSTUM,
    /**
     * An interior segment between the base and taper.
     */
    MIDDLE,
    /**
     * The segment at the formation's support.
     */
    BASE,
    /**
     * A tip joined to a formation pointing in the opposite direction.
     */
    MERGE
}
