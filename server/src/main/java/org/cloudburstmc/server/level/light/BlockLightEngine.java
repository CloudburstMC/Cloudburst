package org.cloudburstmc.server.level.light;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BlockLightEngine {

    public static void initialize(LightAccess access, int chunkX, int chunkZ, LightPropagation into) {
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
        for (int y = access.minHeight(); y < access.maxHeight(); y++) {
            for (int i = 0; i < 16; i++) {
                sourceIfLit(access, into, minX - 1, y, minZ + i);
                sourceIfLit(access, into, minX + 16, y, minZ + i);
                sourceIfLit(access, into, minX + i, y, minZ - 1);
                sourceIfLit(access, into, minX + i, y, minZ + 16);
            }
        }
    }

    private static void sourceIfLit(LightAccess access, LightPropagation into, int x, int y, int z) {
        if (access.isLoaded(x, z) && access.light(x, y, z) > 0) {
            into.source(LightPositions.pack(x, y, z));
        }
    }
}
