package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.nbt.NbtContainerSize;
import org.cloudburstmc.api.nbt.NbtKeepUnlisted;
import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtPositional;
import org.cloudburstmc.api.nbt.internal.NbtParser;
import org.cloudburstmc.nbt.NbtMap;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.ArrayList;
import java.util.List;

public final class ItemContainerDeserializer extends StdDeserializer<ItemContainer> {

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

    /**
     * Whether an in-place read keeps unlisted slots ({@link NbtKeepUnlisted}):
     * entries bind by their {@code Slot} tag and slots the list omits keep
     * their current item instead of being cleared.
     */
    private final boolean keepUnlisted;

    public ItemContainerDeserializer() {
        this(-1, false, false);
    }

    private ItemContainerDeserializer(int size, boolean positional, boolean keepUnlisted) {
        super(ItemContainer.class);
        this.size = size;
        this.positional = positional;
        this.keepUnlisted = keepUnlisted;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        NbtContainerSize annotation =
                property == null ? null : property.getAnnotation(NbtContainerSize.class);
        boolean positional = property != null && property.getAnnotation(NbtPositional.class) != null;
        boolean keepUnlisted =
                property != null && property.getAnnotation(NbtKeepUnlisted.class) != null;
        return new ItemContainerDeserializer(annotation == null ? -1 : annotation.value(), positional,
                keepUnlisted);
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
        List<StagedEntry> staged = positional ? stagePositional(p, list, size)
                : keepUnlisted ? stageKeepUnlisted(p, list, size) : stage(p, list, size);
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
        // read never leaves a partially updated container behind. Sparse
        // player inventories keep the slots the list omits; every other
        // container is cleared first, as its load always was.
        List<StagedEntry> staged = keepUnlisted ? stageKeepUnlisted(p, list, size) : stage(p, list, into.size());
        if (!keepUnlisted) {
            into.clear();
        }
        for (StagedEntry entry : staged) {
            into.setItem(entry.slot(), entry.item());
        }
        return into;
    }

    /**
     * Reads every entry of a container list without touching any container, so the
     * caller can fill or replace contents only after the whole tag proved usable.
     * A non-compound entry throws a mismatch (the read keeps the current value);
     * an item the registry rejects throws its own failure, before anything was
     * applied. The slot binds like the old byte read: only a byte tag binds, so a
     * missing or non-byte {@code Slot} binds slot 0; an index outside the
     * container fails the read with a bare exception, as the old bounds check
     * did, failing the load instead of keeping the current contents with a
     * warning (which would lose the stored items on the next save).
     */
    private static List<StagedEntry> stage(JsonParser p, List<?> list, int size) {
        List<StagedEntry> staged = new ArrayList<>(list.size());
        for (Object entry : list) {
            if (!(entry instanceof NbtMap item)) {
                throw mismatch(p, entry);
            }
            int index = slotIndex(item.get("Slot"));
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(
                        "Slot " + index + " is out of range for a container of size " + size);
            }
            staged.add(new StagedEntry(index,
                    org.cloudburstmc.server.item.ItemUtils.deserializeItem(item)));
        }
        return staged;
    }

    /**
     * Reads the slot the way the old byte read did: a byte tag binds its signed
     * value, anything else, including a missing tag, binds slot 0.
     */
    private static int slotIndex(Object slot) {
        return slot instanceof Byte b ? b : 0;
    }

    /**
     * Reads every entry of a sparse keep-unlisted container list (a player
     * inventory or ender chest) without touching any container, so the caller
     * can fill or replace contents only after the whole tag proved usable. An
     * entry without a {@code Slot} binds slot 0, as today's player load read
     * the slot with a default of zero; an index outside the container fails
     * the read with a bare exception, as the old bounds check did, failing
     * the load instead of keeping the current contents with a warning.
     */
    private static List<StagedEntry> stageKeepUnlisted(JsonParser p, List<?> list, int size) {
        List<StagedEntry> staged = new ArrayList<>(list.size());
        for (Object entry : list) {
            if (!(entry instanceof NbtMap item)) {
                throw mismatch(p, entry);
            }
            Object slot = item.get("Slot");
            // Only a byte tag binds, as the old byte read did: anything else,
            // including a missing tag, binds slot 0.
            int index = slotIndex(slot);
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(
                        "Slot " + index + " is out of range for a container of size " + size);
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
                "Cannot read an item container entry " + NbtTags.describe(entry)
                        + "; expected an item compound");
        mismatch.initCause(new NbtKindMismatchException(
                "Item container entries need an item compound"));
        return mismatch;
    }
}
