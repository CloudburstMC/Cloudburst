package org.cloudburstmc.api.item;

import com.google.common.collect.ImmutableMap;
import com.nukkitx.math.GenericMath;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.data.DataStore;

import java.util.Collections;
import java.util.Map;

public final class ItemStack implements DataStore, Comparable<ItemStack> {

    private final ItemType type;
    private final int amount;
    private final ImmutableMap<DataKey<?, ?>, ?> metadata;

    ItemStack(ItemType type, int amount, Map<DataKey<?, ?>, ?> metadata) {
        this.type = type;
        this.amount = amount;
        this.metadata = ImmutableMap.copyOf(metadata);
    }

    public static ItemStackBuilder builder() {
        return new ItemStackBuilder(null, 1, Collections.emptyMap());
    }

    public ItemType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public ItemStackBuilder toBuilder() {
        return new ItemStackBuilder(this.type, this.amount, this.metadata);
    }

    public ItemStack reduceAmount() {
        return withAmount(this.amount - 1);
    }

    public ItemStack increaseAmount() {
        return addAmount(1);
    }

    public ItemStack addAmount(int delta) {
        return withAmount(this.amount + delta);
    }

    public ItemStack withAmount(int amount) {
        if (this.amount == amount) {
            return this;
        }
        return toBuilder().amount(amount).build();
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(DataKey<T, ?> key) {
        return (T) metadata.get(key);
    }

    @Override
    public int compareTo(ItemStack other) {
        if (other.getType().equals(this.getType())) {
            return this.getAmount() - other.getAmount();
        }
        return this.getType().getId().compareTo(other.getType().getId());
    }
}
