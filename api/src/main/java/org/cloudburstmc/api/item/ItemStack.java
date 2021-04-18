package org.cloudburstmc.api.item;

import com.nukkitx.math.GenericMath;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.behavior.ItemBehavior;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.api.util.Identifier;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@NonNull
public interface ItemStack extends Comparable<ItemStack> {

    @Inject
    ItemRegistry registry = null; //does that work?

    ItemType getType();

    int getAmount();

    default boolean isNull() {
        return true;
    }

    String getName();

    default boolean hasName() {
        return getName() != null;
    }

    List<String> getLore();

    default boolean hasEnchantments() {
        return !getEnchantments().isEmpty();
    }

    Map<EnchantmentType, EnchantmentInstance> getEnchantments();

    default EnchantmentInstance getEnchantment(EnchantmentType enchantment) {
        for (EnchantmentInstance ench : getEnchantments().values()) {
            if (ench.getType() == enchantment) {
                return ench;
            }
        }

        return null;
    }

    Collection<Identifier> getCanDestroy();

    default boolean canDestroy(BlockState state) {
        return getCanDestroy().contains(state.getType().getId());
    }

    Collection<Identifier> getCanPlaceOn();

    default boolean canPlaceOn(BlockState state) {
        return getCanPlaceOn().contains(state.getType().getId());
    }

    default <T> T getMetadata(Class<T> metadataClass) {
        return getMetadata(metadataClass, null);
    }

    <T> T getMetadata(Class<T> metadataClass, T defaultValue);

    boolean hasTag();

    ItemStackBuilder toBuilder();

//    RecipeItemStackBuilder toRecipeBuilder();

    ItemBehavior getBehavior();

    boolean isMergeable(@NonNull ItemStack itemStack);

    boolean equals(@Nullable ItemStack item);

    default boolean isFull() {
        return getAmount() >= getType().getMaximumStackSize();
    }

    default boolean equals(@Nullable ItemStack other, boolean checkAmount) {
        return equals(other, checkAmount, true);
    }

    boolean equals(@Nullable ItemStack other, boolean checkAmount, boolean checkData);

    default ItemStack decrementAmount() {
        return decrementAmount(1);
    }

    default ItemStack decrementAmount(int amount) {
        return withAmount(getAmount() - amount);
    }

    default ItemStack incrementAmount() {
        return incrementAmount(1);
    }

    default ItemStack incrementAmount(int amount) {
        return withAmount(getAmount() + amount);
    }

    default ItemStack withAmount(int amount) {
        if (this.getAmount() == amount) {
            return this;
        }
        return toBuilder().amount(GenericMath.clamp(amount, 0, getBehavior().getMaxStackSize(this))).build();
    }

    default ItemStack withEnchantment(EnchantmentInstance enchantment) {
        return toBuilder().addEnchantment(enchantment).build();
    }

    ItemStack withData(Object data);

    ItemStack withData(Class<?> metadataClass, Object data);

    default BlockState getBlockState() {
        throw new UnsupportedOperationException("Item " + this.getType() + " cannot be converted to a block state");
    }

/*     static ItemStack get(BlockState state) { // Do we need get methods in ItemStack?
        return get(state, 1);
    }

   static ItemStack get(BlockState state, int amount) {
        return this.registry.getItem(state, amount);
    }

    static ItemStack get(ItemType type) {
        return get(type, 1);
    }

    static ItemStack get(ItemType type, int amount, Object... metadata) {
        return ItemRegistry.get().getItem(type, amount, metadata);
    }*/

    @Override
    default int compareTo(ItemStack other) {
        if (other.getType().equals(this.getType())) {
            return this.getAmount() - other.getAmount();
        }
        return this.getType().getId().compareTo(other.getType().getId());
    }
}
