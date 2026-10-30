package org.cloudburstmc.server.nbt;

import org.cloudburstmc.api.blockentity.data.SlottedItem;
import org.cloudburstmc.api.entity.vehicle.BoatType;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.nbt.NbtContainerSize;
import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtPositional;
import org.cloudburstmc.api.nbt.NbtSkipEmpty;
import org.cloudburstmc.api.nbt.internal.NbtParser;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.server.entity.vehicle.VanillaBoats;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.potion.PotionEffectDataSerializer;
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
import tools.jackson.databind.ValueSerializer;
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
        addSerializer(EffectType.class, new EffectTypeSerializer());
        addDeserializer(EffectType.class, new EffectTypeDeserializer());
        addSerializer(BoatType.class, new BoatTypeSerializer());
        addDeserializer(BoatType.class, new BoatTypeDeserializer());
        addSerializer(PotionEffect.class, new PotionEffectSerializer());
        addDeserializer(PotionEffect.class, new PotionEffectDeserializer());
    }

    /**
     * Stores an effect type as its network id in an int, as beacon effects are stored today.
     * An id the mapping does not know fails the read with the mapping's own error, exactly
     * like the previous load path, which converted the stored int after reading it.
     */
    private static final class EffectTypeSerializer extends StdSerializer<EffectType> {

        EffectTypeSerializer() {
            super(EffectType.class);
        }

        @Override
        public void serialize(EffectType value, JsonGenerator gen, SerializationContext provider) {
            gen.writeNumber((int) NetworkUtils.effectToNetwork(value));
        }
    }

    private static final class EffectTypeDeserializer extends StdDeserializer<EffectType> {

        EffectTypeDeserializer() {
            super(EffectType.class);
        }

        @Override
        public EffectType deserialize(JsonParser p, DeserializationContext ctxt) {
            Object tag = p instanceof NbtParser parser ? parser.currentTag() : null;
            if (!(tag instanceof Number number)) {
                MismatchedInputException mismatch = MismatchedInputException.from(p, EffectType.class,
                        "Cannot read an effect type from " + describe(tag) + "; expected an int effect id");
                mismatch.initCause(new NbtKindMismatchException("Effect types are stored as int effect ids"));
                throw mismatch;
            }
            p.skipChildren();
            return NetworkUtils.effectFromNetwork(number.byteValue());
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

        private final boolean skipEmpty;

        ItemContainerSerializer() {
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

    private static final class ItemContainerDeserializer extends StdDeserializer<ItemContainer> {

        /**
         * The slot count of containers this instance creates, from the property's
         * {@link NbtContainerSize}, or {@code -1} when the property declares none.
         */
        private final int size;

        /**
         * Whether entries bind by position ({@link NbtPositional}): the first
         * entry fills slot 0, ignoring any {@code Slot} tag, and a short list
         * leaves the remaining slots untouched instead of clearing them.
         */
        private final boolean positional;

        ItemContainerDeserializer() {
            this(-1, false);
        }

        private ItemContainerDeserializer(int size, boolean positional) {
            super(ItemContainer.class);
            this.size = size;
            this.positional = positional;
        }

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            NbtContainerSize annotation =
                    property == null ? null : property.getAnnotation(NbtContainerSize.class);
            boolean positional = property != null && property.getAnnotation(NbtPositional.class) != null;
            return new ItemContainerDeserializer(annotation == null ? -1 : annotation.value(), positional);
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
            if (!(p instanceof NbtParser parser) || !(parser.currentTag() instanceof List<?> list)) {
                return (ItemContainer) ctxt.handleUnexpectedToken(ItemContainer.class, p);
            }
            p.skipChildren();
            // Stage every entry before the container is created, so a failed
            // read never leaves a partially filled container behind.
            List<StagedEntry> staged = positional ? stagePositional(p, list, size) : stage(p, list, size);
            ItemContainer created = ItemContainers.detached(size);
            for (StagedEntry entry : staged) {
                created.setItem(entry.slot(), entry.item());
            }
            return created;
        }

        @Override
        public ItemContainer deserialize(JsonParser p, DeserializationContext ctxt, ItemContainer into) {
            if (!(p instanceof NbtParser parser) || !(parser.currentTag() instanceof List<?> list)) {
                return (ItemContainer) ctxt.handleUnexpectedToken(ItemContainer.class, p);
            }
            p.skipChildren();
            if (positional) {
                // Creature equipment loads as today: entries fill slots by
                // position, extra entries are ignored, and missing slots keep
                // their current item instead of being cleared.
                List<StagedEntry> staged = stagePositional(p, list, into.size());
                for (StagedEntry entry : staged) {
                    into.setItem(entry.slot(), entry.item());
                }
                return into;
            }
            // Stage every entry before the container is touched, so a failed
            // read never leaves a partially updated container behind.
            List<StagedEntry> staged = stage(p, list, into.size());
            into.clear();
            for (StagedEntry entry : staged) {
                into.setItem(entry.slot(), entry.item());
            }
            return into;
        }

        /**
         * Reads every entry of a container list without touching any container, so the
         * caller can fill or replace contents only after the whole tag proved usable.
         * A shape problem throws a mismatch (the read keeps the current value); an item
         * the registry rejects throws its own failure, before anything was applied.
         */
        private static List<StagedEntry> stage(JsonParser p, List<?> list, int size) {
            List<StagedEntry> staged = new ArrayList<>(list.size());
            for (Object entry : list) {
                if (!(entry instanceof NbtMap item) || !(item.get("Slot") instanceof Number slot)) {
                    throw mismatch(p, entry);
                }
                int index = slot.intValue();
                if (index < 0 || index >= size) {
                    throw rangeMismatch(p, entry, size);
                }
                staged.add(new StagedEntry(index,
                        org.cloudburstmc.server.item.ItemUtils.deserializeItem(item)));
            }
            return staged;
        }

        /**
         * Reads every entry of a positional container list without touching any
         * container: entries fill slots by position, ignoring any {@code Slot}
         * tag, and entries past the container's size are ignored. A failed item
         * read throws before anything was applied.
         */
        private static List<StagedEntry> stagePositional(JsonParser p, List<?> list, int size) {
            List<StagedEntry> staged = new ArrayList<>(Math.min(list.size(), size));
            for (int index = 0; index < list.size() && index < size; index++) {
                Object entry = list.get(index);
                if (!(entry instanceof NbtMap item)) {
                    throw mismatch(p, entry);
                }
                staged.add(new StagedEntry(index,
                        org.cloudburstmc.server.item.ItemUtils.deserializeItem(item)));
            }
            return staged;
        }

        private record StagedEntry(int slot, ItemStack item) {
        }

        private static MismatchedInputException mismatch(JsonParser p, Object entry) {
            MismatchedInputException mismatch = MismatchedInputException.from(p, ItemContainer.class,
                    "Cannot read an item container entry " + describe(entry)
                            + "; expected an item compound with a numeric Slot");
            mismatch.initCause(new NbtKindMismatchException(
                    "Item container entries need an item compound with a numeric Slot"));
            return mismatch;
        }

        private static MismatchedInputException rangeMismatch(JsonParser p, Object entry, int size) {
            MismatchedInputException mismatch = MismatchedInputException.from(p, ItemContainer.class,
                    "Cannot read an item container entry " + describe(entry)
                            + "; expected a Slot between 0 and " + (size - 1));
            mismatch.initCause(new NbtKindMismatchException(
                    "Item container slots must be between 0 and " + (size - 1)));
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

    /**
     * Stores a boat hull as its variant int, as boats are stored today.
     * An id the variant mapping does not know fails the read with the mapping's own
     * error, exactly like the previous load path.
     */
    private static final class BoatTypeSerializer extends StdSerializer<BoatType> {

        BoatTypeSerializer() {
            super(BoatType.class);
        }

        @Override
        public void serialize(BoatType value, JsonGenerator gen, SerializationContext provider) {
            gen.writeNumber(VanillaBoats.definition(value).variant());
        }
    }

    private static final class BoatTypeDeserializer extends StdDeserializer<BoatType> {

        BoatTypeDeserializer() {
            super(BoatType.class);
        }

        @Override
        public BoatType deserialize(JsonParser p, DeserializationContext ctxt) {
            Object tag = p instanceof NbtParser parser ? parser.currentTag() : null;
            if (!(tag instanceof Number number)) {
                MismatchedInputException mismatch = MismatchedInputException.from(p, BoatType.class,
                        "Cannot read a boat type from " + describe(tag) + "; expected an int variant");
                mismatch.initCause(new NbtKindMismatchException("Boat types are stored as int variants"));
                throw mismatch;
            }
            p.skipChildren();
            return VanillaBoats.fromVariant(number.intValue()).type();
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

    /**
     * Stores a potion effect as its data compound, as area effect clouds are stored today.
     * An effect id the mapping does not know fails the read with the mapping's own error,
     * exactly like the previous load path.
     */
    private static final class PotionEffectSerializer extends StdSerializer<PotionEffect> {

        PotionEffectSerializer() {
            super(PotionEffect.class);
        }

        @Override
        public void serialize(PotionEffect value, JsonGenerator gen, SerializationContext provider) {
            gen.writeEmbeddedObject(PotionEffectDataSerializer.serialize(value));
        }
    }

    private static final class PotionEffectDeserializer extends StdDeserializer<PotionEffect> {

        PotionEffectDeserializer() {
            super(PotionEffect.class);
        }

        @Override
        public PotionEffect deserialize(JsonParser p, DeserializationContext ctxt) {
            Object tag = p instanceof NbtParser parser ? parser.currentTag() : null;
            if (!(tag instanceof NbtMap map)) {
                MismatchedInputException mismatch = MismatchedInputException.from(p, PotionEffect.class,
                        "Cannot read a potion effect from " + describe(tag) + "; expected a compound");
                mismatch.initCause(new NbtKindMismatchException("Potion effects are stored as compounds"));
                throw mismatch;
            }
            p.skipChildren();
            return PotionEffectDataSerializer.deserialize(map);
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
