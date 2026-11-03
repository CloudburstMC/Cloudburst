package org.cloudburstmc.server.level;

import org.cloudburstmc.api.level.LevelHeightAccessor;

/**
 * The vertical build limits of a dimension.
 *
 * @param minHeight the lowest valid y, a multiple of 16
 * @param maxHeight one past the highest valid y, a multiple of 16
 */
public record DimensionHeight(int minHeight, int maxHeight) implements LevelHeightAccessor {
    public static final DimensionHeight OVERWORLD = new DimensionHeight(-64, 320);
    public static final DimensionHeight NETHER = new DimensionHeight(0, 128);
    public static final DimensionHeight THE_END = new DimensionHeight(0, 256);

    public DimensionHeight {
        if ((minHeight & 15) != 0 || (maxHeight & 15) != 0 || minHeight >= maxHeight) {
            throw new IllegalArgumentException("invalid dimension height " + minHeight + " to " + maxHeight);
        }
    }

    /**
     * Returns the build limits of the given dimension.
     *
     * @param dimension the dimension id, one of {@link CloudLevel#DIMENSION_OVERWORLD},
     *                  {@link CloudLevel#DIMENSION_NETHER} or {@link CloudLevel#DIMENSION_THE_END}
     * @return the dimension's build limits, defaulting to the overworld for an unknown id
     */
    public static DimensionHeight of(int dimension) {
        return switch (dimension) {
            case CloudLevel.DIMENSION_NETHER -> NETHER;
            case CloudLevel.DIMENSION_THE_END -> THE_END;
            default -> OVERWORLD;
        };
    }

    @Override
    public int getMinHeight() {
        return this.minHeight;
    }

    @Override
    public int getMaxHeight() {
        return this.maxHeight;
    }
}
