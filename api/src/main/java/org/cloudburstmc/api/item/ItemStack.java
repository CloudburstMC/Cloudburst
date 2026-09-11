package org.cloudburstmc.api.item;

import com.google.common.collect.ImmutableMap;
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
 * <p>{@link #EMPTY} is the canonical sentinel for "no item / empty slot". It has the
 * {@link ItemTypes#AIR} type and a count of zero.
 * Always test with {@link #isEmpty()} rather than comparing the type or instance directly.</p>
 * Item types identified as {@code minecraft:air} are canonicalized to {@link #EMPTY} when built.
 *
 * <p>{@link #getType()} always returns a non-null item type.</p>
 */
public final class ItemStack implements DataStore, Comparable<ItemStack> {

    /**
     * Sentinel for "no item". Its type is {@link ItemTypes#AIR} and its count is zero.
     */
    public static final ItemStack EMPTY = new ItemStack(ItemTypes.AIR, 0, Collections.emptyMap());

    private final ItemType type;
    private final int count;
    private final ImmutableMap<DataKey<?, ?>, ?> metadata;

    private ItemStack(ItemType type, int count, Map<DataKey<?, ?>, ?> metadata) {
        this.type = type;
        this.count = count;
        this.metadata = ImmutableMap.copyOf(metadata);
    }

    static ItemStack create(ItemType type, int count, Map<DataKey<?, ?>, ?> metadata) {
        checkNotNull(type, "type");
        checkArgument(count > 0, "count must be positive");
        checkNotNull(metadata, "metadata");

        if (type.isAir()) {
            return EMPTY;
        }
        return new ItemStack(type, count, metadata);
    }

    /**
     * Creates an item stack builder without an item type.
     *
     * @return a new builder
     */
    public static ItemStackBuilder builder() {
        return new ItemStackBuilder(null, 1, Collections.emptyMap());
    }

    /**
     * Creates an item stack builder for an item type.
     *
     * @param type the item type
     * @return a new builder
     */
    public static ItemStackBuilder builder(@NonNull ItemType type) {
        return new ItemStackBuilder(checkNotNull(type, "type"), 1, Collections.emptyMap());
    }

    /**
     * Creates an item stack builder for a block state.
     *
     * @param state the block state represented by the item
     * @return a new builder
     * @throws IllegalArgumentException if the block has no item form
     */
    public static ItemStackBuilder builder(@NonNull BlockState state) {
        checkNotNull(state, "state");
        return new ItemStackBuilder(state.getType().asItem().orElseThrow(
                () -> new IllegalArgumentException("Block " + state.getType().getId() + " has no item form")),
                1, Collections.emptyMap())
                .data(ItemKeys.BLOCK_STATE, state);
    }

    /**
     * Creates one item of the given type.
     *
     * @param type the item type
     * @return the item stack, or {@link #EMPTY} when the type is air
     */
    public static ItemStack from(@NonNull ItemType type) {
        return from(type, 1);
    }

    /**
     * Creates an item stack of the given type.
     *
     * @param type the item type
     * @param amount the positive stack count
     * @return the item stack, or {@link #EMPTY} when the type is air
     * @throws IllegalArgumentException if the amount is not positive
     */
    public static ItemStack from(@NonNull ItemType type, int amount) {
        return create(type, amount, Collections.emptyMap());
    }

    /**
     * Creates one item representing a block state.
     *
     * @param state the block state represented by the item
     * @return the item stack
     * @throws IllegalArgumentException if the block has no item form
     */
    public static ItemStack from(@NonNull BlockState state) {
        return from(state, 1);
    }

    /**
     * Creates an item stack representing a block state.
     *
     * @param state the block state represented by the item
     * @param amount the positive stack count
     * @return the item stack
     * @throws IllegalArgumentException if the amount is not positive or the block has no item form
     */
    public static ItemStack from(@NonNull BlockState state, int amount) {
        checkNotNull(state, "state");
        ItemType itemType = state.getType().asItem().orElseThrow(
                () -> new IllegalArgumentException("Block " + state.getType().getId() + " has no item form"));
        return create(itemType, amount, Map.of(ItemKeys.BLOCK_STATE, state));
    }

    /**
     * Returns {@code true} if this is the {@link #EMPTY} sentinel (no item).
     * Prefer this over comparing the stack instance or item type directly.
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * Returns the item type. Empty stacks have the {@link ItemTypes#AIR} type.
     *
     * @return the item type
     */
    public @NonNull ItemType getType() {
        return type;
    }

    /**
     * Returns the number of items in this stack.
     *
     * @return the stack count, or zero for {@link #EMPTY}
     */
    public int getCount() {
        return count;
    }

    /**
     * Creates a builder initialized from this stack.
     *
     * @return a new builder
     */
    public ItemStackBuilder toBuilder() {
        if (isEmpty()) {
            return builder(ItemTypes.AIR);
        }
        return new ItemStackBuilder(this.type, this.count, this.metadata);
    }

    /**
     * Returns a stack with its count reduced by one.
     *
     * @return the updated stack, or {@link #EMPTY} when no items remain
     */
    public ItemStack decreaseCount() {
        return withCount(count - 1);
    }

    /**
     * Returns a stack with its count reduced by the given amount.
     *
     * @param amount the amount to subtract
     * @return the updated stack, or {@link #EMPTY} when no items remain
     * @throws IllegalArgumentException if the amount is negative
     */
    public ItemStack decreaseCount(int amount) {
        checkArgument(amount >= 0, "amount cannot be negative");
        return withCount(count - amount);
    }

    /**
     * Returns a stack with its count increased by one.
     *
     * @return the updated stack
     */
    public ItemStack increaseCount() {
        return addCount(1);
    }

    /**
     * Returns a stack with its count increased by the given amount.
     *
     * @param amount the amount to add
     * @return the updated stack
     * @throws IllegalArgumentException if the amount is negative
     */
    public ItemStack increaseCount(int amount) {
        checkArgument(amount >= 0, "amount cannot be negative");
        return addCount(amount);
    }

    /**
     * Returns a stack whose count is adjusted by the given delta.
     *
     * @param delta the signed count change
     * @return the updated stack, or {@link #EMPTY} when the resulting count is not positive
     */
    public ItemStack addCount(int delta) {
        return withCount(count + delta);
    }

    /**
     * Returns a stack with the given count.
     *
     * @param amount the new count
     * @return the updated stack, or {@link #EMPTY} when the count is not positive
     */
    public ItemStack withCount(int amount) {
        if (this.count == amount) {
            return this;
        }
        if (amount <= 0) {
            return EMPTY;
        }
        return toBuilder().amount(amount).build();
    }

    /**
     * Returns {@code true} if this stack has durability damage greater than zero.
     *
     * <p>This checks the effective damage value. Use {@link #hasDamageValue()} when you need to know
     * whether a damage value is explicitly stored, including an explicit value of {@code 0}.</p>
     *
     * @return {@code true} if this non-empty stack is damaged
     */
    public boolean hasDamage() {
        return !isEmpty() && getDamage() > 0;
    }

    /**
     * Returns {@code true} if this stack explicitly stores a damage value.
     *
     * <p>An explicit value of {@code 0} still counts as present. Use {@link #resetDamage()} to remove the
     * stored value and fall back to the default undamaged state.</p>
     *
     * @return {@code true} if damage metadata is present
     */
    public boolean hasDamageValue() {
        return !isEmpty() && this.metadata.containsKey(ItemKeys.DAMAGE);
    }

    /**
     * Gets this stack's durability damage.
     *
     * <p>Higher values mean the item is more worn. Missing damage data is treated as {@code 0}, and negative
     * stored values are clamped to {@code 0}.</p>
     *
     * @return the effective durability damage
     */
    public int getDamage() {
        Integer damage = this.get(ItemKeys.DAMAGE);
        return damage == null ? 0 : Math.max(0, damage);
    }

    /**
     * Returns a copy of this stack with an explicit durability damage value.
     *
     * <p>Passing {@code 0} stores an explicit zero damage value. Use {@link #resetDamage()} to remove the
     * damage value instead.</p>
     *
     * @param damage the durability damage to store
     * @return a stack with the requested damage value, or this stack if unchanged
     */
    public ItemStack withDamage(int damage) {
        checkArgument(damage >= 0, "damage cannot be negative");
        if (isEmpty()) {
            return this;
        }

        Integer currentDamage = this.get(ItemKeys.DAMAGE);
        if (Objects.equals(currentDamage, damage)) {
            return this;
        }

        return toBuilder().data(ItemKeys.DAMAGE, damage).build();
    }

    /**
     * Returns a copy of this stack with the explicit damage value removed.
     *
     * <p>The effective damage after reset is {@code 0}, but the metadata entry is not retained.</p>
     *
     * @return a stack without damage metadata, or this stack if no damage value was present
     */
    public ItemStack resetDamage() {
        if (isEmpty() || !hasDamageValue()) {
            return this;
        }

        return toBuilder().removeData(ItemKeys.DAMAGE).build();
    }

    /**
     * Returns a copy of this stack with durability damage increased by {@code amount}.
     *
     * <p>This method only changes the stored damage value. Item-specific break checks, enchantment handling,
     * and side effects are handled by item components such as {@link ItemComponents#ON_DAMAGE}.</p>
     *
     * @param amount the amount of damage to add
     * @return a stack with increased damage, or this stack if unchanged
     */
    public ItemStack damage(int amount) {
        checkArgument(amount >= 0, "amount cannot be negative");
        if (amount == 0 || isEmpty()) {
            return this;
        }

        return withDamage(getDamage() + amount);
    }

    /**
     * Returns a copy of this stack with durability damage reduced by {@code amount}.
     *
     * <p>If the repair amount reaches or exceeds the current damage, the damage value is removed rather than
     * stored as an explicit {@code 0}.</p>
     *
     * @param amount the amount of damage to repair
     * @return a stack with reduced damage, or this stack if unchanged
     */
    public ItemStack repair(int amount) {
        checkArgument(amount >= 0, "amount cannot be negative");
        if (amount == 0 || isEmpty()) {
            return this;
        }

        int currentDamage = getDamage();
        if (amount >= currentDamage) {
            return resetDamage();
        }

        return withDamage(currentDamage - amount);
    }

    /**
     * Returns {@code true} if this stack is marked unbreakable.
     *
     * @return {@code true} when durability damage should not be applied
     */
    public boolean isUnbreakable() {
        return this.get(ItemKeys.UNBREAKABLE) == Boolean.TRUE;
    }

    /**
     * Returns a copy of this stack with the unbreakable flag set or removed.
     *
     * @param unbreakable {@code true} to mark the stack unbreakable, {@code false} to clear the flag
     * @return a stack with the requested unbreakable state, or this stack if unchanged
     */
    public ItemStack withUnbreakable(boolean unbreakable) {
        if (isEmpty()) {
            return this;
        }

        if (isUnbreakable() == unbreakable) {
            return this;
        }

        ItemStackBuilder builder = toBuilder();
        if (!unbreakable) {
            return builder.removeData(ItemKeys.UNBREAKABLE).build();
        }

        return builder.data(ItemKeys.UNBREAKABLE, true).build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(DataKey<T, ?> key) {
        checkNotNull(key, "key");
        Object data = metadata.get(key);
        return data == null ? key.getDefaultValue() : (T) data;
    }

    public ImmutableMap<DataKey<?, ?>, ?> getAllMetadata() {
        return metadata;
    }

    public boolean isBlock() {
        return metadata.containsKey(ItemKeys.BLOCK_STATE);
    }

    public Optional<BlockState> getBlockState() {
        return Optional.ofNullable(this.isBlock() ? this.get(ItemKeys.BLOCK_STATE) : null);
    }

    /**
     * Returns the block state represented by this item.
     *
     * @return the represented block state
     * @throws IllegalStateException if this is not a block item
     */
    public @NonNull BlockState requireBlockState() {
        return getBlockState().orElseThrow(() -> new IllegalStateException("Item stack is not a block item"));
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
    public int compareTo(@NonNull ItemStack other) {
        checkNotNull(other, "other");
        if (this.isEmpty() && other.isEmpty()) return 0;
        if (this.isEmpty()) return -1;
        if (other.isEmpty()) return 1;

        ItemType type = this.type;
        ItemType otherType = other.type;
        if (type.equals(otherType)) {
            return Integer.compare(this.count, other.count);
        }

        return type.getId().compareTo(otherType.getId());
    }

    /**
     * Returns {@code true} if both stacks are the same item type (count and metadata ignored).
     * Two empty stacks are considered similar.
     */
    public boolean isSimilar(@NonNull ItemStack other) {
        checkNotNull(other, "other");
        if (this == other) return true;
        if (this.isEmpty() || other.isEmpty()) return this.isEmpty() == other.isEmpty();
        return type.equals(other.type);
    }

    /**
     * Returns {@code true} if both stacks can be merged into one inventory stack.
     *
     * <p>Stack count is ignored. Metadata is compared by effective value, so missing metadata is equivalent
     * to that key's default value. Block item states are first normalized to the block type's default state
     * so equivalent block items can merge.</p>
     */
    public boolean isStackableWith(@NonNull ItemStack other) {
        checkNotNull(other, "other");
        if (!isSimilar(other)) {
            return false;
        }

        ItemStack left = normalizeBlockState();
        ItemStack right = other.normalizeBlockState();
        if (left.metadata.equals(right.metadata)) {
            return true;
        }

        return effectiveMetadataEquals(left, right);
    }

    /**
     * Returns {@code true} if both stacks have the same item type and exactly equal metadata.
     *
     * <p>Use {@link #isStackableWith(ItemStack)} for inventory merge checks. This method intentionally keeps
     * implicit and explicit default block-state metadata distinct.</p>
     */
    public boolean isSimilarMetadata(@NonNull ItemStack other) {
        return isSimilar(other) && getAllMetadata().equals(other.getAllMetadata());
    }

    private static boolean effectiveMetadataEquals(ItemStack left, ItemStack right) {
        for (DataKey<?, ?> key : left.metadata.keySet()) {
            if (!effectiveMetadataValueEquals(left, right, key)) {
                return false;
            }
        }

        for (DataKey<?, ?> key : right.metadata.keySet()) {
            if (!effectiveMetadataValueEquals(left, right, key)) {
                return false;
            }
        }

        return true;
    }

    private static boolean effectiveMetadataValueEquals(ItemStack left, ItemStack right, DataKey<?, ?> key) {
        if (key == ItemKeys.BLOCK_STATE) {
            return blockStateMetadataEquals(left, right);
        }

        return Objects.equals(effectiveMetadataValue(left, key), effectiveMetadataValue(right, key));
    }

    private static Object effectiveMetadataValue(ItemStack stack, DataKey<?, ?> key) {
        return stack.metadata.containsKey(key) ? stack.metadata.get(key) : key.getDefaultValue();
    }

    private static boolean blockStateMetadataEquals(ItemStack left, ItemStack right) {
        BlockState leftState = left.metadata.containsKey(ItemKeys.BLOCK_STATE) ? left.get(ItemKeys.BLOCK_STATE) : null;
        BlockState rightState = right.metadata.containsKey(ItemKeys.BLOCK_STATE) ? right.get(ItemKeys.BLOCK_STATE) : null;

        if (Objects.equals(leftState, rightState)) {
            return true;
        }

        if (leftState == null) {
            return isDefaultBlockStateForItem(rightState, left.type);
        }

        if (rightState == null) {
            return isDefaultBlockStateForItem(leftState, right.type);
        }

        return false;
    }

    private static boolean isDefaultBlockStateForItem(@Nullable BlockState state, ItemType itemType) {
        return state != null
                && state.equals(state.getType().getDefaultState())
                && state.getType().asItem().filter(itemType::equals).isPresent();
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
