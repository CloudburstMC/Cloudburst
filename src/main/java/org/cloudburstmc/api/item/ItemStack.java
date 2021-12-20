package org.cloudburstmc.api.item;

import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.index.qual.NonNegative;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.data.DataStore;

import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class ItemStack implements DataStore, Comparable<ItemStack> {

    public static final ItemStack AIR = new ItemStack(BlockTypes.AIR, 0, Collections.emptyMap());

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

    public static ItemStackBuilder builder(BlockState state) {
        return new ItemStackBuilder(state.getType(), 1, Collections.emptyMap())
                .data(ItemKeys.BLOCK_STATE, state);
    }

    public static ItemStackBuilder builder(ItemType type) {
        return new ItemStackBuilder(type, 1, Collections.emptyMap());
    }

    public static ItemStack from(BlockState state) {
        return from(state, 1);
    }

    public static ItemStack from(BlockState state, @NonNegative int amount) {
        checkNotNull(state, "state");
        checkArgument(amount > 0, "Amount cannot be negative");
        return new ItemStack(state.getType(), amount, Map.of(ItemKeys.BLOCK_STATE, state));
    }

    public static ItemStack from(ItemType type) {
        return from(type, 1);
    }

    public static ItemStack from(ItemType type, @NonNegative int amount) {
        checkNotNull(type, "type");
        checkArgument(amount > 0, "Amount cannot be negative");
        return new ItemStack(type, amount, Collections.emptyMap());
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

    public BlockState getBlockState() {
        return getData(ItemKeys.BLOCK_STATE);
    }

    @Override
    public int compareTo(ItemStack other) {
        if (other.getType().equals(this.getType())) {
            return this.getAmount() - other.getAmount();
        }
        return this.getType().getId().compareTo(other.getType().getId());
    }
}
