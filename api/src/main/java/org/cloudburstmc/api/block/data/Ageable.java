package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A block with a numeric age or growth stage.
 * Ages use the block type's own state values.
 */
public interface Ageable extends BlockData {

    /**
     * @return the current age
     */
    int getAge();

    /**
     * @return the greatest supported age for this block type
     */
    int getMaximumAge();

    /**
     * @param age the replacement age, from zero through {@link #getMaximumAge()}
     * @return a state with the requested age
     * @throws IllegalArgumentException if the age is outside the supported range
     */
    BlockState withAge(int age);
}
