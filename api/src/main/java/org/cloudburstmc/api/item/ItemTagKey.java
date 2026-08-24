package org.cloudburstmc.api.item;

import lombok.Value;
import org.cloudburstmc.api.util.Identifier;

import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifies a named item tag.
 */
@Value
public class ItemTagKey {

    private static final ConcurrentHashMap<Identifier, ItemTagKey> KEYS = new ConcurrentHashMap<>();

    Identifier id;

    private ItemTagKey(Identifier id) {
        this.id = id;
    }

    public static ItemTagKey of(String id) {
        return of(Identifier.parse(id));
    }

    public static ItemTagKey of(Identifier id) {
        checkNotNull(id, "id");
        return KEYS.computeIfAbsent(id, ItemTagKey::new);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
