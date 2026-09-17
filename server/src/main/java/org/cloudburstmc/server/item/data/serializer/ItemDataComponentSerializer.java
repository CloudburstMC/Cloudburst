package org.cloudburstmc.server.item.data.serializer;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

/**
 * Serializes one item data component to and from item tag NBT.
 *
 * @param <T> component value type
 */
public interface ItemDataComponentSerializer<T> {

    /**
     * Writes the component value into the item's network tag.
     *
     * @param item  the item being serialized
     * @param tag   the mutable item tag
     * @param value component value to write
     */
    void serialize(ItemStack item, NbtMapBuilder tag, T value);

    /**
     * Reads the component value from item tag NBT.
     *
     * @return component value, or {@code null} when this serializer's NBT is absent
     */
    T deserialize(Identifier id, NbtMap tag);
}
