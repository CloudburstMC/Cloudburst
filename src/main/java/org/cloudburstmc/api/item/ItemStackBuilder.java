package org.cloudburstmc.api.item;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.util.Identifier;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static lombok.Lombok.checkNotNull;
import static org.cloudburstmc.api.item.ItemKeys.BLOCK_STATE;

public final class ItemStackBuilder {

    private ItemType itemType;
    private int amount;
    private final Map<DataKey<?, ?>, Object> metadata;

    ItemStackBuilder(ItemType itemType, int amount, Map<DataKey<?, ?>, ?> metadata) {
        this.itemType = itemType;
        this.amount = amount;
        this.metadata = new IdentityHashMap<>(metadata);
    }

    public ItemStackBuilder itemType(@NonNull ItemType itemType) {
        checkNotNull(itemType, "itemType is null");
        this.itemType = itemType;
        return this;
    }

    public ItemStackBuilder amount(@NonNegative int amount) {
        checkArgument(amount > 0, "amount cannot be less than zero");
        this.amount = amount;
        return this;
    }

    public ItemStackBuilder clearData() {
        this.metadata.clear();
        return this;
    }

    public <T, M> ItemStackBuilder data(DataKey<T, M> key, M value) {
        checkNotNull(key, "key");
        checkNotNull(value, "value");
        this.metadata.put(key, key.getImmutableFunction().apply(value));
        return this;
    }

    public ItemStack build() {
        checkNotNull(this.itemType, "itemType is null");
        checkArgument(this.amount > 0, "amount cannot be less than zero");
        checkArgument(!(this.itemType instanceof BlockType) || this.metadata.containsKey(BLOCK_STATE),
                "ItemStack with a BlockType requires BlockState data");
        return new ItemStack(itemType, amount, metadata);
    }
}
