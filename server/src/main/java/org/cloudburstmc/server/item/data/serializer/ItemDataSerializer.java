package org.cloudburstmc.server.item.data.serializer;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

/**
 * Serializes one item metadata value to and from item tag NBT.
 *
 * @param <T> the immutable metadata value type
 */
public interface ItemDataSerializer<T> {

    /**
     * Writes the metadata value into the item's network tag.
     *
     * @param item the item being serialized
     * @param tag the mutable item tag
     * @param value the metadata value to write
     */
    void serialize(ItemStack item, NbtMapBuilder tag, T value);

    /**
     * Reads this metadata value from item tag NBT.
     *
     * @return the metadata value, or {@code null} when this serializer's NBT is absent
     */
    T deserialize(Identifier id, NbtMap tag);
}
