package org.cloudburstmc.api.level;

/**
 * Exposes the vertical build limits of a dimension and provides derived helpers
 * for converting between world Y coordinates and chunk-section indices.
 */
public interface LevelHeightAccessor {

    /**
     * Returns the lowest valid world Y coordinate.
     */
    int getMinHeight();

    /**
     * Returns one past the highest valid world Y coordinate.
     */
    int getMaxHeight();

    /**
     * Returns the number of 16-block chunk sections that span this dimension's
     * height range, i.e. {@code (getMaxHeight() - getMinHeight()) >> 4}.
     */
    default int getSectionsCount() {
        return (getMaxHeight() - getMinHeight()) >> 4;
    }

    /**
     * Returns the section Y of the bottom-most section, i.e.
     * {@code getMinHeight() >> 4}.
     */
    default int getMinSectionY() {
        return getMinHeight() >> 4;
    }

    /**
     * Converts a world Y coordinate to a 0-based section-array index.
     *
     * <p>Formula: {@code (y - getMinHeight()) >> 4}. The result is undefined
     * if {@code y} is outside {@code [getMinHeight(), getMaxHeight())}.
     */
    default int getSectionIndex(int y) {
        return (y - getMinHeight()) >> 4;
    }

    /**
     * Returns {@code true} if {@code y} is strictly outside
     * {@code [getMinHeight(), getMaxHeight())}.
     */
    default boolean isOutsideBuildHeight(int y) {
        return y < getMinHeight() || y >= getMaxHeight();
    }
}
