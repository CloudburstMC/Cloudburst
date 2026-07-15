package org.cloudburstmc.api.block;

import lombok.Value;
import org.cloudburstmc.api.util.Identifier;

import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifies a named block tag.
 */
@Value
public class BlockTagKey {

    private static final ConcurrentHashMap<Identifier, BlockTagKey> KEYS = new ConcurrentHashMap<>();

    Identifier id;

    private BlockTagKey(Identifier id) {
        this.id = id;
    }

    public static BlockTagKey of(String id) {
        return of(Identifier.parse(id));
    }

    public static BlockTagKey of(Identifier id) {
        checkNotNull(id, "id");
        return KEYS.computeIfAbsent(id, BlockTagKey::new);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
