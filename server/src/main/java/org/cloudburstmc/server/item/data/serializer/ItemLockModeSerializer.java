package org.cloudburstmc.server.item.data.serializer;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.data.ItemLockMode;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

public class ItemLockModeSerializer implements ItemDataComponentSerializer<ItemLockMode> {
    private static final String TAG_ITEM_LOCK = "minecraft:item_lock";
    private static final byte LOCK_IN_SLOT_VALUE = 1;
    private static final byte LOCK_IN_INVENTORY_VALUE = 2;

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, ItemLockMode value) {
        tag.putByte(TAG_ITEM_LOCK, switch (value) {
            case LOCK_IN_SLOT -> LOCK_IN_SLOT_VALUE;
            case LOCK_IN_INVENTORY -> LOCK_IN_INVENTORY_VALUE;
        });
    }

    @Override
    public ItemLockMode deserialize(Identifier id, NbtMap tag) {
        return switch (tag.getByte(TAG_ITEM_LOCK, (byte) 0)) {
            case LOCK_IN_SLOT_VALUE -> ItemLockMode.LOCK_IN_SLOT;
            case LOCK_IN_INVENTORY_VALUE -> ItemLockMode.LOCK_IN_INVENTORY;
            default -> null;
        };
    }
}
