package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.util.Direction;

public final class LightPositions {

    private LightPositions() {
    }

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

    public static long offset(long packed, Direction direction) {
        return pack(x(packed) + direction.getStepX(), y(packed) + direction.getStepY(), z(packed) + direction.getStepZ());
    }
}
