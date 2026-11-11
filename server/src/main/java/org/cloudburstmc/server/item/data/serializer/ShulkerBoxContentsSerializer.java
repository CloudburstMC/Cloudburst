package org.cloudburstmc.server.item.data.serializer;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.data.ShulkerBoxContents;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.item.ShulkerBoxItems;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the indexed items stored by a shulker box.
 */
public class ShulkerBoxContentsSerializer implements ItemDataComponentSerializer<ShulkerBoxContents> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, ShulkerBoxContents value) {
        if (ShulkerBoxItems.TYPES.contains(item.getType())) {
            write(tag, value);
        }
    }

    @Override
    public @Nullable ShulkerBoxContents deserialize(Identifier id, NbtMap tag) {
        if (!ShulkerBoxItems.isBox(id)) {
            return null;
        }
        ShulkerBoxContents contents = read(tag);
        return contents.items().isEmpty() ? null : contents;
    }

    public static void write(NbtMapBuilder tag, ShulkerBoxContents contents) {
        List<NbtMap> items = new ArrayList<>();
        contents.items().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            ItemStack item = entry.getValue();
            if (ShulkerBoxItems.accepts(item)) {
                items.add(ItemStackNbtCodec.serializeItem(item, entry.getKey()).toBuilder()
                        .putBoolean("WasPickedUp", false).build());
            }
        });

        if (!items.isEmpty()) {
            tag.putList("Items", NbtType.COMPOUND, items);
        }
    }

    public static ShulkerBoxContents read(NbtMap tag) {
        Map<Integer, ItemStack> items = new HashMap<>();
        for (NbtMap entry : tag.getList("Items", NbtType.COMPOUND, List.of())) {
            int slot = entry.getByte("Slot", (byte) -1);
            if (slot < 0 || slot >= ShulkerBoxContents.SLOT_COUNT || items.containsKey(slot)
                    || ShulkerBoxItems.isBox(Identifier.parse(entry.getString("Name", "minecraft:air")))) {
                continue;
            }

            ItemStack item = ItemStackNbtCodec.deserializeItem(entry);
            if (!item.isEmpty() && ShulkerBoxItems.accepts(item)) {
                items.put(slot, item);
            }
        }

        return new ShulkerBoxContents(items);
    }
}
