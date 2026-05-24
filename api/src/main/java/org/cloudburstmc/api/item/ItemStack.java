package org.cloudburstmc.api.item;

import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.data.DataStore;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An immutable value representing an item type, stack count, and optional metadata.
 * Use {@link #builder()} or one of the {@code from} factory methods to create instances.
 *
 * <p>{@link #EMPTY} is the canonical sentinel for "no item / empty slot".
 * Always test with {@link #isEmpty()} rather than {@code == EMPTY} or {@code getType() == null}.</p>
 *
 * <p>Contract: for any non-empty {@code ItemStack}, {@link #getType()} is guaranteed non-null.
 * {@link #getType()} returns {@code null} only on {@link #EMPTY}.</p>
 */
public final class ItemStack implements DataStore, Comparable<ItemStack> {

    /**
     * Sentinel for "no item". {@link #isEmpty()} returns {@code true}.
     * {@link #getType()} returns {@code null} on this instance only.
     */
    public static final ItemStack EMPTY = new ItemStack(null, 0, Collections.emptyMap());

    @Nullable
    private final ItemType type;
    private final int count;
    private final ImmutableMap<DataKey<?, ?>, ?> metadata;

    ItemStack(@Nullable ItemType type, int count, Map<DataKey<?, ?>, ?> metadata) {
        this.type = type;
        this.count = count;
        this.metadata = ImmutableMap.copyOf(metadata);
    }

    public static ItemStackBuilder builder() {
        return new ItemStackBuilder(null, 1, Collections.emptyMap());
    }

    public static ItemStackBuilder builder(BlockState state) {
        return new ItemStackBuilder(state.getType().asItem().orElseThrow(
                () -> new IllegalArgumentException("Block " + state.getType().getId() + " has no item form")),
                1, Collections.emptyMap())
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
        ItemType itemType = state.getType().asItem().orElseThrow(
                () -> new IllegalArgumentException("Block " + state.getType().getId() + " has no item form"));
        return new ItemStack(itemType, amount, Map.of(ItemKeys.BLOCK_STATE, state));
    }

    public static ItemStack from(ItemType type) {
        return from(type, 1);
    }

    public static ItemStack from(ItemType type, @NonNegative int amount) {
        checkNotNull(type, "type");
        checkArgument(amount > 0, "Amount cannot be negative");
        return new ItemStack(type, amount, Collections.emptyMap());
    }

    /**
     * Returns {@code true} if this is the {@link #EMPTY} sentinel (no item).
     * Prefer this over {@code this == ItemStack.EMPTY} or {@code getType() == null}.
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * Returns the item type, or {@code null} if and only if {@link #isEmpty()} is {@code true}.
     * For non-empty stacks this is always non-null.
     */
    @Nullable
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
        return withCount(count - 1);
    }

    public ItemStack decreaseCount(int amount) {
        return withCount(count - amount);
    }

    public ItemStack increaseCount() {
        return addCount(1);
    }

    public ItemStack increaseCount(int amount) {
        return addCount(amount);
    }

    public ItemStack addCount(int delta) {
        return withCount(count + delta);
    }

    public ItemStack withCount(int amount) {
        if (this.count == amount) {
            return this;
        }
        if (amount <= 0) {
            return EMPTY;
        }
        return toBuilder().amount(amount).build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(DataKey<T, ?> key) {
        checkNotNull(key, "key");
        Object data = metadata.get(key);
        return data == null ? key.getDefaultValue() : (T) data;
    }

    public Optional<BlockState> getBlockState() {
        return Optional.ofNullable(this.isBlock() ? this.get(ItemKeys.BLOCK_STATE) : null);
    }

    public ImmutableMap<DataKey<?, ?>, ?> getAllMetadata() {
        return metadata;
    }

    @Nullable
    public BlockState getEnsuringBlockState() {
        if (!this.isBlock()) {
            throw new NullPointerException("Current Item isn't a block so it can't have a BlockState.");
        }
        return this.get(ItemKeys.BLOCK_STATE);
    }

    public boolean isBlock() {
        return metadata.containsKey(ItemKeys.BLOCK_STATE);
    }

    @Override
    public int compareTo(@NonNull ItemStack other) {
        if (this.isEmpty() && other.isEmpty()) return 0;
        if (this.isEmpty()) return -1;
        if (other.isEmpty()) return 1;
        if (other.type.equals(this.type)) {
            return this.count - other.count;
        }
        return this.type.getId().compareTo(other.type.getId());
    }

    /**
     * Returns {@code true} if both stacks are the same item type (count and metadata ignored).
     * Two empty stacks are considered similar.
     */
    public boolean isSimilar(@NonNull ItemStack other) {
        if (this == other) return true;
        if (this.isEmpty() || other.isEmpty()) return this.isEmpty() == other.isEmpty();
        return this.type.equals(other.type);
    }

    public boolean isSimilarMetadata(@NonNull ItemStack other) {
        return isSimilar(other) && getAllMetadata().equals(other.getAllMetadata());
    }

    /**
     * Returns a copy of this item with the stored {@link BlockState} replaced by the block type's
     * default state, stripping placement-specific properties (e.g. {@code axis}, {@code facing}).
     * If this item is not a block item, or already uses the default state, returns {@code this}.
     */
    public ItemStack normalizeBlockState() {
        if (!isBlock()) {
            return this;
        }
        BlockState current = this.get(ItemKeys.BLOCK_STATE);
        if (current == null) {
            return this;
        }
        BlockState defaultState = current.getType().getDefaultState();
        if (current.equals(defaultState)) {
            return this;
        }
        return toBuilder().data(ItemKeys.BLOCK_STATE, defaultState).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemStack other)) return false;
        return this.count == other.count &&
                Objects.equals(this.type, other.type) &&
                Objects.equals(this.metadata, other.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, count, metadata);
    }

    @Override
    public String toString() {
        if (isEmpty()) return "ItemStack{EMPTY}";
        return "ItemStack{type=" + type.getId() + ", count=" + count + ", metadata=" + metadata + "}";
    }
}
