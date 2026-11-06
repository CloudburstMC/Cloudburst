package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.item.ItemStack;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public final class ItemStackSerializer extends StdSerializer<ItemStack> {

    public ItemStackSerializer() {
        super(ItemStack.class);
    }

    @Override
    public void serialize(ItemStack value, JsonGenerator gen, SerializationContext provider) {
        gen.writeEmbeddedObject(org.cloudburstmc.server.item.ItemUtils.serializeItem(value));
    }
}
