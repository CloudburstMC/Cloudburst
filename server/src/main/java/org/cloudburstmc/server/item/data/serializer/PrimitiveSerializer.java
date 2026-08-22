package org.cloudburstmc.server.item.data.serializer;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

/**
 * Serializes simple scalar metadata values stored directly on the item tag.
 *
 * @param <T> the expected NBT value type
 */
public class PrimitiveSerializer<T> implements ItemDataSerializer<T> {

    private final String key;
    private final Class<T> type;

    /**
     * Creates a serializer for a single tag key.
     *
     * @param key the NBT tag key
     * @param type the expected value type; boolean values also accept Bedrock byte booleans when reading
     */
    public PrimitiveSerializer(String key, Class<T> type) {
        this.key = key;
        this.type = type;
    }

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, T value) {
        tag.put(key, value);
    }

    @Override
    public T deserialize(Identifier id, NbtMap tag) {
        var value = tag.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof Byte num && type == Boolean.class) {
            value = num == 1;
        } else if (value.getClass() != type) {
            return null;
        }

        return type.cast(value);
    }
}
