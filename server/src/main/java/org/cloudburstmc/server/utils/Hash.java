package org.cloudburstmc.server.utils;

public class Hash {
    public static long hashBlock(int x, int y, int z) {
        return ((long) (y + 64) & 0x1FF) | (((long) x & 0x3FFFFFF) << 9) | (((long) z & 0x3FFFFFF) << 35);
    }

    public static int hashBlockX(long triple) {
        return (int) ((((triple >> 9) & 0x3FFFFFF) << 38) >> 38);
    }

    public static int hashBlockY(long triple) {
        return (int) (triple & 0x1FF) - 64;
    }

    public static int hashBlockZ(long triple) {
        return (int) ((((triple >> 35) & 0x3FFFFFF) << 38) >> 38);
    }
}
