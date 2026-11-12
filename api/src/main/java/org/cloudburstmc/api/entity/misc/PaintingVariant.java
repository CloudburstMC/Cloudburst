package org.cloudburstmc.api.entity.misc;

/**
 * Built-in painting artwork and its dimensions in blocks.
 */
public enum PaintingVariant {
    KEBAB(1, 1), AZTEC(1, 1), ALBAN(1, 1), AZTEC2(1, 1), BOMB(1, 1),
    PLANT(1, 1), WASTELAND(1, 1), MEDITATIVE(1, 1),
    WANDERER(1, 2), GRAHAM(1, 2), PRAIRIE_RIDE(1, 2),
    POOL(2, 1), COURBET(2, 1), SUNSET(2, 1), SEA(2, 1), CREEBET(2, 1),
    MATCH(2, 2), BUST(2, 2), STAGE(2, 2), VOID(2, 2), SKULL_AND_ROSES(2, 2),
    WITHER(2, 2), BAROQUE(2, 2), HUMBLE(2, 2), EARTH(2, 2), WIND(2, 2), WATER(2, 2), FIRE(2, 2),
    BOUQUET(3, 3), CAVEBIRD(3, 3), COTAN(3, 3), ENDBOSS(3, 3), FERN(3, 3),
    OWLEMONS(3, 3), SUNFLOWERS(3, 3), TIDES(3, 3), DENNIS(3, 3),
    BACKYARD(3, 4), POND(3, 4),
    FIGHTERS(4, 2), CHANGING(4, 2), FINDING(4, 2), LOWMIST(4, 2), PASSAGE(4, 2),
    SKELETON(4, 3), DONKEY_KONG(4, 3),
    POINTER(4, 4), PIGSCENE(4, 4), BURNING_SKULL(4, 4), ORB(4, 4), UNPACKED(4, 4);

    private final int width;
    private final int height;

    PaintingVariant(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the width in blocks.
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Returns the height in blocks.
     */
    public int getHeight() {
        return this.height;
    }
}
