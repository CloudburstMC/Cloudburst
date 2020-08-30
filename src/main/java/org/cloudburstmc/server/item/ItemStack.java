package org.cloudburstmc.server.item;

import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Nonnull
@Immutable
public interface ItemStack {

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

    Collection<Enchantment> getEnchantments();

    Collection<Identifier> getCanDestroy();

    Collection<Identifier> getCanPlaceOn();

    Optional<Object> getMetadata();

    ItemStackBuilder toBuilder();

//    RecipeItemStackBuilder toRecipeBuilder();

    boolean isSimilar(@Nonnull ItemStack itemStack);

    boolean isMergeable(@Nonnull ItemStack itemStack);

    boolean equals(@Nullable ItemStack item);

    default boolean isFull() {
        return getAmount() >= getType().getMaximumStackSize();
    }

    boolean equals(@Nullable ItemStack other, boolean checkAmount, boolean checkMeta, boolean checkUserData);
}
