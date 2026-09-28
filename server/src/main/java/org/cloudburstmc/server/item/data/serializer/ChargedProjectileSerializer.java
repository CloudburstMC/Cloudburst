package org.cloudburstmc.server.item.data.serializer;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;

public class ChargedProjectileSerializer implements ItemDataComponentSerializer<ItemStack> {

    private static final String TAG_CHARGED_ITEM = "chargedItem";

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, ItemStack value) {
        if (!value.isEmpty()) {
            tag.putCompound(TAG_CHARGED_ITEM, ItemUtils.serializeItem(value));
        }
    }

    @Override
    public ItemStack deserialize(Identifier id, NbtMap tag) {
        if (!tag.containsKey(TAG_CHARGED_ITEM, NbtType.COMPOUND)) {
            return null;
        }

        ItemStack loaded = ItemUtils.deserializeItem(tag.getCompound(TAG_CHARGED_ITEM));
        return loaded.isEmpty() ? null : loaded;
    }
}
