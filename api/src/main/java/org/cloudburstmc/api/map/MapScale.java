package org.cloudburstmc.api.map;

/**
 * The horizontal number of world blocks represented by one map pixel.
 */
public enum MapScale {
    /**
     * One block per pixel.
     */
    CLOSEST(1),
    /**
     * Two blocks per pixel.
     */
    CLOSE(2),
    /**
     * Four blocks per pixel.
     */
    NORMAL(4),
    /**
     * Eight blocks per pixel.
     */
    FAR(8),
    /**
     * Sixteen blocks per pixel.
     */
    FARTHEST(16);

    private final int blocksPerPixel;

    MapScale(int blocksPerPixel) {
        this.blocksPerPixel = blocksPerPixel;
    }

    /**
     * @return the number of world blocks along each pixel's edge
     */
    public int getBlocksPerPixel() {
        return this.blocksPerPixel;
    }
}
