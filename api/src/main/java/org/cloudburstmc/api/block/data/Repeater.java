package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A redstone repeater's facing, powered state and propagation delay.
 */
public interface Repeater extends Directional, Powerable {

    /**
     * @return the delay in redstone ticks, each lasting two game ticks
     */
    int getDelay();

    /**
     * @return the smallest supported delay in redstone ticks
     */
    int getMinimumDelay();

    /**
     * @return the greatest supported delay in redstone ticks
     */
    int getMaximumDelay();

    /**
     * @param delay the replacement delay within the supported bounds
     * @return a state with the requested delay
     * @throws IllegalArgumentException if the delay is outside the supported range
     */
    BlockState withDelay(int delay);
}
