package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.item.ItemStack;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public final class ItemStackDeserializer extends StdDeserializer<ItemStack> {

    public ItemStackDeserializer() {
        super(ItemStack.class);
    }

    @Override
    public ItemStack deserialize(JsonParser p, DeserializationContext ctxt) {
        return org.cloudburstmc.server.item.ItemUtils.deserializeItem(NbtTags.readTag(p, ctxt));
    }
}
