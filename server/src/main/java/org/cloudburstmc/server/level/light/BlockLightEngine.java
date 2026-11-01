package org.cloudburstmc.server.level.light;

import lombok.experimental.UtilityClass;

/**
 * Writes a chunk's block light from scratch and seeds propagation.
 */
@UtilityClass
class BlockLightEngine {

    /**
     * Writes the emission of the band's columns and sources the band's
     * emitters. The border sources run with the last band, once every column
     * holds its emission.
     *
     * @param band  the column band from {@code 0} to {@code bands - 1}
     * @param bands the number of column bands; must be positive
     */
    static void initializeBand(LightAccess access, int chunkX, int chunkZ, int band, int bands,
            LightPropagation into) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int fromX = LightPositions.bandFromX(band, bands);
        int toX = LightPositions.bandToX(band, bands);
        for (int x = fromX; x < toX; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = access.minHeight(); y < access.maxHeight(); y++) {
                    int emission = access.states().emission(
                            access.primary(minX + x, y, minZ + z), access.secondary(minX + x, y, minZ + z));
                    if (access.light(minX + x, y, minZ + z) != emission) {
                        access.setLight(minX + x, y, minZ + z, emission);
                    }
                    if (emission > 0) {
                        into.source(LightPositions.pack(minX + x, y, minZ + z));
                    }
                }
            }
        }
        if (band == bands - 1) {
            LightPositions.forNeighbourBorder(chunkX, chunkZ, access.minHeight(), access.maxHeight(),
                    (x, y, z, intoChunk) -> sourceIfLit(access, into, x, y, z));
        }
    }

    private static void sourceIfLit(LightAccess access, LightPropagation into, int x, int y, int z) {
        if (access.isLoaded(x, z) && access.light(x, y, z) > 0) {
            into.source(LightPositions.pack(x, y, z));
        }
    }
}
