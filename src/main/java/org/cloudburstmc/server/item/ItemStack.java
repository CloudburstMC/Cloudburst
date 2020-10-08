package org.cloudburstmc.server.item;

import com.nukkitx.math.GenericMath;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentType;
import org.cloudburstmc.server.item.behavior.ItemBehavior;
import org.cloudburstmc.server.registry.ItemRegistry;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Nonnull
@Immutable
public interface ItemStack /*extends ItemBehavior*/ {

    @Inject
    ItemRegistry registry = null; //does that work?

    ItemType getType();

    int getAmount();

    @Deprecated
    default int getCount() {
        return getAmount();
    }

    default boolean isNull() {
        return this.getType() == BlockTypes.AIR || this.getAmount() <= 0;
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
        return getCanDestroy().contains(state.getId());
    }

    Collection<Identifier> getCanPlaceOn();

    default boolean canPlaceOn(BlockState state) {
        return getCanPlaceOn().contains(state.getId());
    }

    default <T> T getMetadata(Class<T> metadataClass) {
        return getMetadata(metadataClass, null);
    }

    <T> T getMetadata(Class<T> metadataClass, T defaultValue);

    ItemStackBuilder toBuilder();

//    RecipeItemStackBuilder toRecipeBuilder();

    ItemBehavior getBehavior();

    boolean isMergeable(@Nonnull ItemStack itemStack);

    boolean equals(@Nullable ItemStack item);

    default boolean isFull() {
        return getAmount() >= getType().getMaximumStackSize();
    }

    boolean equals(@Nullable ItemStack other, boolean checkAmount, boolean checkData);

    default ItemStack decrementAmount() {
        return decrementAmount(1);
    }

    default ItemStack decrementAmount(int amount) {
        return toBuilder().amount(GenericMath.clamp(getAmount() - amount, 0, getBehavior().getMaxStackSize(this))).build();
    }

    default ItemStack incrementAmount() {
        return incrementAmount(1);
    }

    default ItemStack incrementAmount(int amount) {
        return toBuilder().amount(GenericMath.clamp(getAmount() + amount, 0, getBehavior().getMaxStackSize(this))).build();
    }

    default ItemStack withAmount(int amount) {
        return toBuilder().amount(GenericMath.clamp(amount, 0, getBehavior().getMaxStackSize(this))).build();
    }

    ItemStack withData(Object data);

    ItemStack withData(Class<?> metadataClass, Object data);

    static ItemStack get(BlockState state) {
        return get(state, 1);
    }

    static ItemStack get(BlockState state, int amount) {
        return registry.getItem(state, amount);
    }

    static ItemStack get(ItemType type, Object... metadata) {
        return get(type, 1, metadata);
    }

    static ItemStack get(ItemType type, int amount, Object... metadata) {
        return registry.getItem(type, amount, metadata);
    }
}
