package org.cloudburstmc.server.item;

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
import java.util.Optional;

@Nonnull
@Immutable
public interface ItemStack {

    @Inject
    ItemRegistry registry = null; //does that work?

    ItemType getType();

    int getAmount();

    static boolean isInvalid(@Nullable ItemStack item) {
        return isNull(item) || item.getAmount() <= 0;
    }

    static boolean isNull(@Nullable ItemStack item) {
        return item == null || item.getType() == BlockTypes.AIR;
    }

    Optional<String> getName();

    List<String> getLore();

    Collection<EnchantmentInstance> getEnchantments();

    default EnchantmentInstance getEnchantment(EnchantmentType enchantment) {
        for (EnchantmentInstance ench : getEnchantments()) {
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

    <T> Optional<T> getMetadata(Class<T> metadataClass);

    ItemStackBuilder toBuilder();

//    RecipeItemStackBuilder toRecipeBuilder();

    ItemBehavior getBehavior();

    boolean isMergeable(@Nonnull ItemStack itemStack);

    boolean equals(@Nullable ItemStack item);

    default boolean isFull() {
        return getAmount() >= getType().getMaximumStackSize();
    }

    boolean equals(@Nullable ItemStack other, boolean checkAmount, boolean checkData);

    static ItemStack get(ItemType type) {
        return registry.getItem(type);
    }
}
