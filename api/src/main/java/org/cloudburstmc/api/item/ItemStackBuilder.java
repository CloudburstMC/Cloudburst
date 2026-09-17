package org.cloudburstmc.api.item;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Mutable builder for immutable {@link ItemStack} instances.
 *
 * <p>The builder only stores the item type, amount, and explicit data components supplied by the caller.
 * Block item state is not inferred from the item type; use {@link ItemStack#builder(org.cloudburstmc.api.block.BlockState)}
 * or set {@link ItemDataComponents#BLOCK_STATE} explicitly when creating a block item stack.</p>
 */
public class ItemStackBuilder {

    private final Map<ItemDataComponentType<?>, Object> dataComponents;
    private ItemType itemType;
    private int amount;

    ItemStackBuilder(@Nullable ItemType itemType, int amount, Map<ItemDataComponentType<?>, ?> dataComponents) {
        this.itemType = itemType;
        this.amount = amount;
        this.dataComponents = new LinkedHashMap<>(checkNotNull(dataComponents, "dataComponents"));
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
     * Stores an explicit data component value.
     *
     * @param type  the component type
     * @param value component value
     * @return this builder
     */
    public <T> ItemStackBuilder setData(ItemDataComponentType<T> type, T value) {
        checkNotNull(type, "type");
        checkNotNull(value, "value");
        this.dataComponents.put(type, type.copyValue(value));
        return this;
    }

    /**
     * Removes an explicitly stored data component from the stack being built.
     *
     * @param type component type to remove
     * @return this builder
     */
    public ItemStackBuilder removeData(@NonNull ItemDataComponentType<?> type) {
        checkNotNull(type, "type");
        this.dataComponents.remove(type);
        return this;
    }

    /**
     * Removes all explicitly stored data components from the stack being built.
     *
     * @return this builder
     */
    public ItemStackBuilder clearData() {
        this.dataComponents.clear();
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
        return ItemStack.create(itemType, amount, dataComponents);
    }
}
