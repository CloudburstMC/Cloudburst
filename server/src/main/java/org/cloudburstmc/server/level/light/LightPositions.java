package org.cloudburstmc.server.level.light;

import lombok.experimental.UtilityClass;

/**
 * Packs block positions into longs: 26 bits of x, 26 bits of z and 12 bits of y.
 *
 * <p>Valid ranges are x and z within +-2^25 and y within +-2^11; packing outside
 * these ranges wraps.
 */
@UtilityClass
public class LightPositions {

    public static long pack(int x, int y, int z) {
        return ((long) x << 38) | ((z & 0x3FFFFFFL) << 12) | (y & 0xFFFL);
    }

    public static int x(long packed) {
        return (int) (packed >> 38);
    }

    public static int y(long packed) {
        return (int) (packed << 52 >> 52);
    }

    public static int z(long packed) {
        return (int) (packed << 26 >> 38);
    }
}
