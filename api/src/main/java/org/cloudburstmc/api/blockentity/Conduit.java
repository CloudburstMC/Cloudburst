package org.cloudburstmc.api.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.util.BoundingBox;

import java.util.Collection;

/**
 * A conduit that grants conduit power and attacks nearby creatures when fully framed.
 */
public interface Conduit extends BlockEntity {

    /**
     * Activation requires at least 16 valid frame blocks and water throughout
     * the surrounding 3x3x3 area, including water contained in blocks.
     *
     * @return whether the surrounding water and frame activate this conduit
     */
    boolean isActive();

    /**
     * Hunting requires all 42 frame positions to contain valid frame blocks.
     *
     * @return whether this conduit is active and has a complete hunting frame
     */
    boolean isHunting();

    /**
     * Returns the valid frame blocks found by the most recent structure check.
     * The returned collection is immutable.
     *
     * @return the frame blocks
     */
    Collection<Block> getFrameBlocks();

    /**
     * @return the number of valid frame blocks found by the most recent check
     */
    int getFrameBlockCount();

    /**
     * @return the effect radius in blocks, or zero when inactive
     */
    int getRange();

    /**
     * @return the world-space area searched for creatures to attack
     */
    BoundingBox getHuntingArea();

    /**
     * @return the current living target, or {@code null} when none is valid
     */
    @Nullable
    Living getTarget();

    /**
     * @return whether this conduit currently has a valid living target
     */
    boolean hasTarget();

    /**
     * Changes the hunting target without immediately attacking it. Non-null targets must
     * be alive, in this level and within eight blocks of an active hunting conduit.
     * Clearing the target allows the conduit to select another creature.
     *
     * @param target the target, or {@code null} to clear it
     * @return whether the target changed
     */
    boolean setTarget(@Nullable Living target);
}
