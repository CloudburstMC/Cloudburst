package org.cloudburstmc.api.item;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.data.DataKey;

import java.util.IdentityHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Mutable builder for immutable {@link ItemStack} instances.
 *
 * <p>The builder only stores the item type, amount, and explicit metadata supplied by the caller.
 * Block item state is not inferred from the item type; use {@link ItemStack#builder(org.cloudburstmc.api.block.BlockState)}
 * or set {@link ItemKeys#BLOCK_STATE} explicitly when creating a block item stack.</p>
 */
public final class ItemStackBuilder {

    private final Map<DataKey<?, ?>, Object> metadata;
    private ItemType itemType;
    private int amount;

    ItemStackBuilder(@Nullable ItemType itemType, int amount, Map<DataKey<?, ?>, ?> metadata) {
        this.itemType = itemType;
        this.amount = amount;
        this.metadata = new IdentityHashMap<>(checkNotNull(metadata, "metadata"));
    }

    /**
     * Sets the item type.
     *
     * @param itemType the item type
     * @return this builder
     */
    public ItemStackBuilder itemType(@NonNull ItemType itemType) {
        checkNotNull(itemType, "itemType is null");
        this.itemType = itemType;
        return this;
    }

    /**
     * Sets the stack amount.
     *
     * @param amount the positive stack amount
     * @return this builder
     */
    public ItemStackBuilder amount(int amount) {
        checkArgument(amount > 0, "amount must be positive");
        this.amount = amount;
        return this;
    }

    /**
     * Stores an explicit metadata value.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @return this builder
     */
    public <T, M> ItemStackBuilder data(DataKey<T, M> key, M value) {
        checkNotNull(key, "key");
        checkNotNull(value, "value");
        this.metadata.put(key, key.getImmutableFunction().apply(value));
        return this;
    }

    /**
     * Removes an explicitly stored metadata value from the stack being built.
     *
     * <p>After removal, reads for the key use the key's default value.</p>
     *
     * @param key the metadata key to remove
     * @return this builder
     */
    public ItemStackBuilder removeData(@NonNull DataKey<?, ?> key) {
        checkNotNull(key, "key");
        this.metadata.remove(key);
        return this;
    }

    /**
     * Removes all explicitly stored metadata values from the stack being built.
     *
     * @return this builder
     */
    public ItemStackBuilder clearData() {
        this.metadata.clear();
        return this;
    }

    /**
     * Builds an immutable item stack.
     *
     * @return the built stack, or {@link ItemStack#EMPTY} when the item type is air
     * @throws NullPointerException     if no item type has been set
     * @throws IllegalArgumentException if the amount is not positive
     */
    public ItemStack build() {
        return ItemStack.create(itemType, amount, metadata);
    }
}
