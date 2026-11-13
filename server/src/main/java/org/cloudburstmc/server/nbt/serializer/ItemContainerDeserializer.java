package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.nbt.NbtKindMismatchException;
import org.cloudburstmc.server.nbt.annotation.NbtContainerSize;
import org.cloudburstmc.server.nbt.annotation.NbtKeepUnlisted;
import org.cloudburstmc.server.nbt.annotation.NbtPositional;
import org.cloudburstmc.server.nbt.internal.NbtParser;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.ArrayList;
import java.util.List;

public class ItemContainerDeserializer extends StdDeserializer<ItemContainer> {

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
        NbtContainerSize annotation = property == null ? null : property.getAnnotation(NbtContainerSize.class);
        boolean positional = property != null && property.getAnnotation(NbtPositional.class) != null;
        boolean keepUnlisted = property != null && property.getAnnotation(NbtKeepUnlisted.class) != null;
        return new ItemContainerDeserializer(annotation == null ? -1 : annotation.value(), positional, keepUnlisted);
    }

    @Override
    public Boolean supportsUpdate(DeserializationConfig config) {
        return Boolean.TRUE;
    }

    @Override
    public ItemContainer deserialize(JsonParser p, DeserializationContext ctxt) {
        if (size < 0) {
            throw new IllegalArgumentException("Cannot read an item container without an existing instance."
                    + " Declare its slot count with @NbtContainerSize on the property");
        }

        if (!(p instanceof NbtParser parser) || !(parser.currentTag() instanceof List<?> list)) {
            return (ItemContainer) ctxt.handleUnexpectedToken(ItemContainer.class, p);
        }
        p.skipChildren();

        List<StagedEntry> entries = this.decodeEntries(p, list, this.size);
        ItemContainer created = new CloudContainer(this.size);
        apply(created, entries);
        return created;
    }

    @Override
    public ItemContainer deserialize(JsonParser p, DeserializationContext ctxt, ItemContainer into) {
        if (!(p instanceof NbtParser parser) || !(parser.currentTag() instanceof List<?> list)) {
            return (ItemContainer) ctxt.handleUnexpectedToken(ItemContainer.class, p);
        }
        p.skipChildren();

        List<StagedEntry> entries = this.decodeEntries(p, list, into.size());
        if (!this.positional && !this.keepUnlisted) {
            into.clear();
        }

        apply(into, entries);
        return into;
    }

    private List<StagedEntry> decodeEntries(JsonParser parser, List<?> list, int slotCount) {
        int entryCount = this.positional ? Math.min(list.size(), slotCount) : list.size();
        List<StagedEntry> entries = new ArrayList<>(entryCount);

        int position = 0;
        for (Object entry : list) {
            if (position == entryCount) {
                break;
            }
            if (!(entry instanceof NbtMap item)) {
                throw mismatch(parser, entry);
            }

            int slot = this.positional ? position : item.getByte("Slot");

            if (slot < 0 || slot >= slotCount) {
                throw new IndexOutOfBoundsException("Slot " + slot + " is out of range for a container of size " + slotCount);
            }

            entries.add(new StagedEntry(slot, ItemStackNbtCodec.deserializeItem(item)));
            position++;
        }

        return entries;
    }

    private static void apply(ItemContainer container, List<StagedEntry> entries) {
        for (StagedEntry entry : entries) {
            container.setItem(entry.slot(), entry.item());
        }
    }

    private static MismatchedInputException mismatch(JsonParser p, Object entry) {
        MismatchedInputException mismatch = MismatchedInputException.from(p, ItemContainer.class,
                "Cannot read an item container entry " + NbtTags.describe(entry) + ". Expected an item compound");
        mismatch.initCause(new NbtKindMismatchException("Item container entries need an item compound"));
        return mismatch;
    }

    private record StagedEntry(int slot, ItemStack item) {
    }
}
