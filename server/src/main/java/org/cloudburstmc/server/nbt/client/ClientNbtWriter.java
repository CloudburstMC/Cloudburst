package org.cloudburstmc.server.nbt.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.nbt.NbtMapper;

/**
 * Writes only fields explicitly marked for inclusion in client tags.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientNbtWriter {

    public static NbtMap write(NbtMapper mapper, Object value) {
        return mapper.write(value, NbtClient.class);
    }
}
