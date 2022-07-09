package org.cloudburstmc.server.item.data.serializer;

import com.google.common.base.Preconditions;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

public class PrimitiveSerializer<T> implements ItemDataSerializer<T> {

    private final String key;
    private final Class<T> type;

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
        } else {
            Preconditions.checkArgument(value.getClass() == type);
        }

        return (T) value;
    }
}
