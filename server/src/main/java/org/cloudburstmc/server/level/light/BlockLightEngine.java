package org.cloudburstmc.server.level.light;

import lombok.experimental.UtilityClass;

/**
 * Writes a chunk's block light from scratch and seeds propagation.
 */
@UtilityClass
class BlockLightEngine {

    /**
     * Writes every block of the chunk to its emission, sources every emitter,
     * and sources lit neighbour cells touching the chunk border.
     */
    static void initialize(LightAccess access, int chunkX, int chunkZ, LightPropagation into) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        for (int x = 0; x < 16; x++) {
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
        LightRegion.forNeighbourBorder(chunkX, chunkZ, access.minHeight(), access.maxHeight(),
                (x, y, z, intoChunk) -> sourceIfLit(access, into, x, y, z));
    }

    private static void sourceIfLit(LightAccess access, LightPropagation into, int x, int y, int z) {
        if (access.isLoaded(x, z) && access.light(x, y, z) > 0) {
            into.source(LightPositions.pack(x, y, z));
        }
    }
}
