package org.cloudburstmc.server.nbt;

import org.cloudburstmc.api.blockentity.data.SlottedItem;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.nbt.internal.NbtParser;
import org.cloudburstmc.nbt.NbtMap;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Maps item stacks and slotted items to their persisted NBT form.
 * <p>
 * The format is whatever {@code ItemUtils} writes today: one compound holding the
 * item's fields, plus a byte {@code Slot} tag for a slotted item.
 */
public class ItemStackNbtModule extends SimpleModule {

    public ItemStackNbtModule() {
        super("cloudburst-item-stack");
        addSerializer(ItemStack.class, new ItemStackSerializer());
        addDeserializer(ItemStack.class, new ItemStackDeserializer());
        addSerializer(SlottedItem.class, new SlottedItemSerializer());
        addDeserializer(SlottedItem.class, new SlottedItemDeserializer());
    }

    private static final class ItemStackSerializer extends StdSerializer<ItemStack> {

        ItemStackSerializer() {
            super(ItemStack.class);
        }

        @Override
        public void serialize(ItemStack value, JsonGenerator gen, SerializationContext provider) {
            gen.writeEmbeddedObject(org.cloudburstmc.server.item.ItemUtils.serializeItem(value));
        }
    }

    private static final class ItemStackDeserializer extends StdDeserializer<ItemStack> {

        ItemStackDeserializer() {
            super(ItemStack.class);
        }

        @Override
        public ItemStack deserialize(JsonParser p, DeserializationContext ctxt) {
            return org.cloudburstmc.server.item.ItemUtils.deserializeItem(readTag(p, ctxt));
        }
    }

    private static final class SlottedItemSerializer extends StdSerializer<SlottedItem> {

        SlottedItemSerializer() {
            super(SlottedItem.class);
        }

        @Override
        public void serialize(SlottedItem value, JsonGenerator gen, SerializationContext provider) {
            gen.writeEmbeddedObject(
                    org.cloudburstmc.server.item.ItemUtils.serializeItem(value.item(), value.slot()));
        }
    }

    private static final class SlottedItemDeserializer extends StdDeserializer<SlottedItem> {

        SlottedItemDeserializer() {
            super(SlottedItem.class);
        }

        @Override
        public SlottedItem deserialize(JsonParser p, DeserializationContext ctxt) {
            NbtMap tag = readTag(p, ctxt);
            return new SlottedItem(tag.getByte("Slot"),
                    org.cloudburstmc.server.item.ItemUtils.deserializeItem(tag));
        }
    }

    private static NbtMap readTag(JsonParser p, DeserializationContext ctxt) {
        if (p instanceof NbtParser parser && parser.currentTag() instanceof NbtMap tag) {
            p.skipChildren();
            return tag;
        }
        return ctxt.readValue(p, NbtMap.class);
    }
}
