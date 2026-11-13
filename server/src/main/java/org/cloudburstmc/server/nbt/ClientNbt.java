package org.cloudburstmc.server.nbt;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.nbt.NbtMap;

/**
 * Writes only fields explicitly marked for inclusion in client tags.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientNbt {

    public static NbtMap write(NbtMapper mapper, Object value) {
        return mapper.write(value, NbtClient.class);
    }
}
