package org.cloudburstmc.server.item;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.ToString;
import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;

@ToString
@Immutable
public class CloudItemStack implements ItemStack {

    private final ItemType type;
    private final int amount;
    private final Object data;
    private final String itemName;
    private final List<String> itemLore;
    private final Set<Enchantment> enchantments;
    private final Set<Identifier> canDestroy;
    private final Set<Identifier> canPlaceOn;

    public CloudItemStack(ItemType type) {
        this(type, 1, null, null, null, null, null, null);
    }

    public CloudItemStack(ItemType type, int amount, Object data) {
        this(type, amount, data, null, null, null, null, null);
    }

    public CloudItemStack(
            ItemType type,
            int amount,
            Object data,
            String itemName,
            List<String> itemLore,
            Collection<Enchantment> enchantments,
            Collection<Identifier> canDestroy,
            Collection<Identifier> canPlaceOn
    ) {
        this.type = type;
        this.amount = amount;
        this.data = data;
        this.itemName = itemName;
        this.itemLore = itemLore == null ? new ArrayList<>() : itemLore;
        this.enchantments = enchantments == null ? ImmutableSet.of() : ImmutableSet.copyOf(enchantments);
        this.canDestroy = canDestroy == null ? ImmutableSet.of() : ImmutableSet.copyOf(canDestroy);
        this.canPlaceOn = canPlaceOn == null ? ImmutableSet.of() : ImmutableSet.copyOf(canPlaceOn);
    }

    @Override
    public ItemType getType() {
        return type;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public Optional<Object> getMetadata() {
        return Optional.ofNullable(data);
    }

    @Override
    public Optional<String> getName() {
        return Optional.ofNullable(itemName);
    }

    @Override
    public List<String> getLore() {
        return ImmutableList.copyOf(itemLore);
    }

    @Override
    public Collection<Enchantment> getEnchantments() {
        return ImmutableList.copyOf(enchantments);
    }

    @Override
    public Collection<Identifier> getCanDestroy() {
        return this.canDestroy;
    }

    @Override
    public Set<Identifier> getCanPlaceOn() {
        return this.canPlaceOn;
    }

    @Override
    public ItemStackBuilder toBuilder() {
        return new CloudItemStackBuilder(this);
    }

//    @Override
//    public RecipeItemStackBuilder toRecipeBuilder() {
//        return new NukkitRecipeItemStackBuilder(this);
//    }

    @Override
    public boolean isMergeable(@Nonnull ItemStack other) {
        return equals(other, false, true, true);
    }

    @Override
    public boolean equals(@Nullable ItemStack item) {
        return equals(item, true, true, true);
    }

    @Override
    public boolean isSimilar(@Nonnull ItemStack other) {
        return equals(other, false, true, false);
    }

    @Override
    public boolean equals(@Nullable ItemStack other, boolean checkAmount, boolean checkMeta, boolean checkUserData) {
        if (this == other) return true;
        if (other == null) return false;
        CloudItemStack that = (CloudItemStack) other;
        return this.type == that.type && (!checkAmount || this.amount == that.amount) &&
                (!checkUserData || Objects.equals(this.itemName, that.itemName) && Objects.equals(this.enchantments, that.enchantments)) &&
                (!checkMeta || Objects.equals(this.data, that.data)); // TODO: Custom data.
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ItemStack)) return false;
        return equals((ItemStack) o, true, true, true);
    }

    public int hashCode() {
        return Objects.hash(type, amount, data, itemName, itemLore, enchantments);
    }
}
