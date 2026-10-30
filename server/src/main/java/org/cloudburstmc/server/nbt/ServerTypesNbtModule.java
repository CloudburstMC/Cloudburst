package org.cloudburstmc.server.nbt;

import org.cloudburstmc.api.blockentity.data.SlottedItem;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.nbt.NbtContainerSize;
import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.internal.NbtParser;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps the runtime types data objects hold to their persisted NBT form: item stacks,
 * slotted items, item containers and, through per-field codecs, colours and positions.
 * <p>
 * The container format is whatever {@code ItemUtils} writes today: a list with one
 * compound per slot holding the item's fields, plus a byte {@code Slot} tag. Empty
 * slots are kept as air compounds, so the saved list always has one entry per slot.
 */
public class ServerTypesNbtModule extends SimpleModule {

    public ServerTypesNbtModule() {
        super("cloudburst-server-types");
        addSerializer(ItemStack.class, new ItemStackSerializer());
        addDeserializer(ItemStack.class, new ItemStackDeserializer());
        addSerializer(SlottedItem.class, new SlottedItemSerializer());
        addDeserializer(SlottedItem.class, new SlottedItemDeserializer());
        addSerializer(ItemContainer.class, new ItemContainerSerializer());
        addDeserializer(ItemContainer.class, new ItemContainerDeserializer());
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

    private static final class ItemContainerSerializer extends StdSerializer<ItemContainer> {

        ItemContainerSerializer() {
            super(ItemContainer.class);
        }

        @Override
        public void serialize(ItemContainer value, JsonGenerator gen, SerializationContext provider) {
            List<NbtMap> items = new ArrayList<>(value.size());
            for (int slot = 0; slot < value.size(); slot++) {
                items.add(org.cloudburstmc.server.item.ItemUtils.serializeItem(value.getItem(slot), slot));
            }
            gen.writeEmbeddedObject(new NbtList<>(NbtType.COMPOUND, items));
        }
    }

    private static final class ItemContainerDeserializer extends StdDeserializer<ItemContainer> {

        /**
         * The slot count of containers this instance creates, from the property's
         * {@link NbtContainerSize}, or {@code -1} when the property declares none.
         */
        private final int size;

        ItemContainerDeserializer() {
            this(-1);
        }

        private ItemContainerDeserializer(int size) {
            super(ItemContainer.class);
            this.size = size;
        }

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            NbtContainerSize annotation =
                    property == null ? null : property.getAnnotation(NbtContainerSize.class);
            return new ItemContainerDeserializer(annotation == null ? -1 : annotation.value());
        }

        @Override
        public Boolean supportsUpdate(DeserializationConfig config) {
            return Boolean.TRUE;
        }

        @Override
        public ItemContainer deserialize(JsonParser p, DeserializationContext ctxt) {
            if (size < 0) {
                throw new IllegalArgumentException("Cannot read an item container with no instance to fill;"
                        + " declare its slot count with @NbtContainerSize on the property");
            }
            return deserialize(p, ctxt, ItemContainers.detached(size));
        }

        @Override
        public ItemContainer deserialize(JsonParser p, DeserializationContext ctxt, ItemContainer into) {
            if (!(p instanceof NbtParser parser) || !(parser.currentTag() instanceof List<?> list)) {
                return (ItemContainer) ctxt.handleUnexpectedToken(ItemContainer.class, p);
            }
            p.skipChildren();
            // The whole tag's shape is validated before the container is touched, so a
            // mismatch never leaves a partially updated container behind.
            for (Object entry : list) {
                if (!(entry instanceof NbtMap item) || !(item.get("Slot") instanceof Number)) {
                    throw mismatch(p, entry);
                }
            }
            into.clear();
            for (Object entry : list) {
                NbtMap item = (NbtMap) entry;
                into.setItem(((Number) item.get("Slot")).intValue(),
                        org.cloudburstmc.server.item.ItemUtils.deserializeItem(item));
            }
            return into;
        }

        private static MismatchedInputException mismatch(JsonParser p, Object entry) {
            MismatchedInputException mismatch = MismatchedInputException.from(p, ItemContainer.class,
                    "Cannot read an item container entry " + describe(entry)
                            + "; expected an item compound with a numeric Slot");
            mismatch.initCause(new NbtKindMismatchException(
                    "Item container entries need an item compound with a numeric Slot"));
            return mismatch;
        }

        private static String describe(Object tag) {
            return switch (tag) {
                case List<?> list -> "a list of " + list.size() + " values";
                case NbtMap ignored -> "a compound";
                case Number ignored -> "a number";
                case String value -> "\"" + value + "\"";
                case null -> "nothing";
                default -> "an NBT " + tag.getClass().getSimpleName();
            };
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
