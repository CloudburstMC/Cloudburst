package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class ItemStackDeserializer extends StdDeserializer<ItemStack> {

    public ItemStackDeserializer() {
        super(ItemStack.class);
    }

    @Override
    public ItemStack deserialize(JsonParser p, DeserializationContext ctxt) {
        return ItemStackNbtCodec.deserializeItem(NbtTags.readTag(p, ctxt));
    }
}
