package org.cloudburstmc.api.entity.misc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * An End Crystal.
 */
public interface EnderCrystal extends Entity {

    /**
     * Returns whether the bedrock base is displayed.
     *
     * @return whether the base is displayed
     */
    boolean isShowingBase();

    /**
     * Sets whether the bedrock base is displayed.
     *
     * @param showingBase whether the base is displayed
     */
    void setShowingBase(boolean showingBase);

    /**
     * Returns the block targeted by the crystal beam.
     *
     * @return the beam target, or {@code null} when no beam is shown
     */
    @Nullable
    Vector3i getBeamTarget();

    /**
     * Changes the block targeted by the crystal beam.
     *
     * @param target the beam target, or {@code null} to hide the beam
     */
    void setBeamTarget(@Nullable Vector3i target);
}
