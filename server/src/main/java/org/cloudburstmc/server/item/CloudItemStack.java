package org.cloudburstmc.server.item;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
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
    private final String itemName;
    private final List<String> itemLore;
    private final Set<Enchantment> enchantments;
    private final Set<Identifier> canDestroy;
    private final Set<Identifier> canPlaceOn;

    private final NbtMap nbt;
    private volatile ItemData networkData;

    public CloudItemStack(ItemType type) {
        this(type, 1, null, null, null, null, null, null, null);
    }

    public CloudItemStack(ItemType type, int amount) {
        this(type, amount, null, null, null, null, null, null, null);
    }

    public CloudItemStack(
            ItemType type,
            int amount,
            String itemName,
            List<String> itemLore,
            Collection<Enchantment> enchantments,
            Collection<Identifier> canDestroy,
            Collection<Identifier> canPlaceOn,
            NbtMap nbt,
            ItemData networkData
    ) {
        this.type = type;
        this.amount = amount;
        this.itemName = itemName;
        this.itemLore = itemLore == null ? new ArrayList<>() : itemLore;
        this.enchantments = enchantments == null ? ImmutableSet.of() : ImmutableSet.copyOf(enchantments);
        this.canDestroy = canDestroy == null ? ImmutableSet.of() : ImmutableSet.copyOf(canDestroy);
        this.canPlaceOn = canPlaceOn == null ? ImmutableSet.of() : ImmutableSet.copyOf(canPlaceOn);
        this.nbt = nbt;
        this.networkData = networkData;
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
    public <T> Optional<T> getMetadata(Class<T> metadataClass) {
        return Optional.empty();
    }

    @Override
    public <T> T ensureMetadata(Class<T> metadataClass) {
        return null;
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

    public ItemData getNetworkData() {
        if (networkData == null) {
            synchronized (this) {
                if (networkData == null) {
                    //TODO: create network data
                }
            }
        }
        return networkData;
    }

    @Override
    public boolean isMergeable(@Nonnull ItemStack other) {
        return equals(other, false, true);
    }

    @Override
    public boolean equals(@Nullable ItemStack item) {
        return equals(item, true, true);
    }

    @Override
    public boolean isSimilar(@Nonnull ItemStack other) {
        return equals(other, false, true);
    }

    @Override
    public boolean equals(@Nullable ItemStack other, boolean checkAmount, boolean checkData) {
        if (this == other) return true;
        if (other == null) return false;
        CloudItemStack that = (CloudItemStack) other;

        return this.type == that.type && (!checkAmount || this.amount == that.amount) &&
                (!checkData || Objects.equals(this.nbt, that.nbt));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ItemStack)) return false;
        return equals((ItemStack) o, true, true);
    }

    public int hashCode() {
        return Objects.hash(type, amount, itemName, itemLore, enchantments);
    }
}
