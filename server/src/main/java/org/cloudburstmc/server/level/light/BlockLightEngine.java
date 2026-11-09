package org.cloudburstmc.server.level.light;

import lombok.experimental.UtilityClass;

/**
 * Writes a chunk's block light from scratch and seeds propagation.
 */
@UtilityClass
public class BlockLightEngine {

    /**
     * Initializes a column band's emission and queues its light sources.
     * Call every band in ascending order before propagating the queued sources.
     *
     * @param band  the column-band index, starting at zero
     * @param bands the positive number of column bands
     */
    public static void initializeBand(LightAccess access, int chunkX, int chunkZ, int band, int bands, LightPropagation into) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int fromX = LightPositions.bandFromX(band, bands);
        int toX = LightPositions.bandToX(band, bands);

        for (int x = fromX; x < toX; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = access.minHeight(); y < access.maxHeight(); y++) {
                    int emission = access.states().emission(
                            access.primary(minX + x, y, minZ + z),
                            access.secondary(minX + x, y, minZ + z)
                    );

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
            LightPositions.forNeighbourBorder(chunkX, chunkZ, access.minHeight(), access.maxHeight(), (x, y, z, intoChunk) -> sourceIfLit(access, into, x, y, z));
        }
    }

    private static void sourceIfLit(LightAccess access, LightPropagation into, int x, int y, int z) {
        if (access.isLoaded(x, z) && access.light(x, y, z) > 0) {
            into.source(LightPositions.pack(x, y, z));
        }
    }
}
