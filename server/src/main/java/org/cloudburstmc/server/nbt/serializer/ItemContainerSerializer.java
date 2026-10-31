package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.nbt.NbtSkipEmpty;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes an item container in the format {@code ItemUtils} writes: a list with one
 * compound per slot holding the item's fields, plus a byte {@code Slot} tag. Empty
 * slots are kept as air compounds, so the saved list has one entry per slot, unless
 * the property is marked {@link NbtSkipEmpty}.
 */
public final class ItemContainerSerializer extends StdSerializer<ItemContainer> {

    private final boolean skipEmpty;

    public ItemContainerSerializer() {
        this(false);
    }

    private ItemContainerSerializer(boolean skipEmpty) {
        super(ItemContainer.class);
        this.skipEmpty = skipEmpty;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
        boolean skipEmpty = property != null && property.getAnnotation(NbtSkipEmpty.class) != null;
        return new ItemContainerSerializer(skipEmpty);
    }

    @Override
    public void serialize(ItemContainer value, JsonGenerator gen, SerializationContext provider) {
        List<NbtMap> items = new ArrayList<>(value.size());
        for (int slot = 0; slot < value.size(); slot++) {
            ItemStack item = value.getItem(slot);
            if (skipEmpty && item.isEmpty()) {
                continue;
            }
            items.add(org.cloudburstmc.server.item.ItemUtils.serializeItem(item, slot));
        }
        gen.writeEmbeddedObject(new NbtList<>(NbtType.COMPOUND, items));
    }
}
