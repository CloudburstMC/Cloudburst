package org.cloudburstmc.api.item;

import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
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
    private final int count;
    private final ImmutableMap<DataKey<?, ?>, ?> metadata;

    ItemStack(ItemType type, int count, Map<DataKey<?, ?>, ?> metadata) {
        this.type = type;
        this.count = count;
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

    public int getCount() {
        return count;
    }

    public ItemStackBuilder toBuilder() {
        return new ItemStackBuilder(this.type, this.count, this.metadata);
    }

    public ItemStack decreaseCount() {
        return withCount(-1);
    }

    public ItemStack decreaseCount(int count) {
        return withCount(-count);
    }

    public ItemStack increaseCount() {
        return addCount(1);
    }

    public ItemStack increaseCount(int count) {
        return addCount(count);
    }

    public ItemStack addCount(int delta) {
        return withCount(this.count + delta);
    }

    public ItemStack withCount(int amount) {
        if (this.count == amount) {
            return this;
        }
        return toBuilder().amount(amount).build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(DataKey<T, ?> key) {
        return (T) metadata.get(key);
    }

    @Nullable
    public BlockState getBlockState() {
        return isBlock() ? get(ItemKeys.BLOCK_STATE) : null;
    }

    public boolean isBlock() {
        return type instanceof BlockType;
    }

    @Override
    public int compareTo(ItemStack other) {
        if (other.getType().equals(this.getType())) {
            return this.getCount() - other.getCount();
        }
        return this.getType().getId().compareTo(other.getType().getId());
    }
}
