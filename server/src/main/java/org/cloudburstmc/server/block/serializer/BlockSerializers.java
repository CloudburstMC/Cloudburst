package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.utils.Identifier;

public class BlockSerializers {

    private static final int LATEST_VERSION = makeVersion(1, 16, 0);

    public static void serializeCommon(NbtMapBuilder builder, Identifier id) {
        builder.putInt("version", LATEST_VERSION)
                .putString("name", id.toString());
    }

    public static int makeVersion(int major, int minor, int patch) {
        return (patch << 8) | (minor << 16) | (major << 26);
    }
}
