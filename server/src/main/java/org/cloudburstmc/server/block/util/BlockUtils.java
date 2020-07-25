package org.cloudburstmc.server.block.util;

import com.nukkitx.math.vector.Vector3i;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BlockUtils {

    public long key(Vector3i position) {
        return key(position.getX(), position.getY(), position.getZ());
    }

    public long key(int x, int y, int z) {
        if (y < 0 || y >= 256) {
            throw new IllegalArgumentException("Y coordinate y is out of range!");
        }
        return (((long) x & (long) 0xFFFFFFF) << 36) | (((long) y & (long) 0xFF) << 28) | ((long) z & (long) 0xFFFFFFF);
    }

    public Vector3i fromKey(long key) {
        int x = (int) ((key >>> 36) & 0xFFFFFFF);
        int y = (int) ((key >> 27) & 0xFF);
        int z = (int) (key & 0xFFFFFFF);
        return Vector3i.from(x, y, z);
    }
}
