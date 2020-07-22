package org.cloudburstmc.server.math;

import javax.annotation.Nonnull;

public enum SimpleDirection {
    NORTH,
    SOUTH,
    EAST,
    WEST;

    public BlockFace toBlockFace() {
        return BlockFace.fromIndex(ordinal() + 2);
    }

    public static SimpleDirection fromIndex(int index) {
        return values()[index];
    }

    public static SimpleDirection fromBlockFace(@Nonnull BlockFace face) {
        int index = face.ordinal() - 2;

        if (index < 0 || index > values().length) {
            return null;
        }

        return values()[index];
    }
}
