package org.cloudburstmc.api.util.data;

/**
 * @author CreeperFace
 */
public enum Bucket {
    EMPTY,
    MILK,
    COD,
    SALMON,
    TROPICAL_FISH,
    PUFFERFISH,
    WATER,
    LAVA;

    @Override
    public final String toString() {
        return "Bucket(" +
                "content=" + name() +
                ')';
    }
}
