package org.cloudburstmc.server.item;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import lombok.ToString;
import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Nonnull
@ToString
public class CloudItemStackBuilder implements ItemStackBuilder {
    private ItemType itemType;
    private int amount = 1;
    private Object data;
    private String itemName;
    private List<String> itemLore;
    private final Set<Enchantment> enchantments = new HashSet<>();
    private final Set<Identifier> canDestroy = new HashSet<>();
    private final Set<Identifier> canPlaceOn = new HashSet<>();
    private NbtMap nbt;
    private ItemData networkData;

    public CloudItemStackBuilder() {
    }

    public CloudItemStackBuilder(@Nonnull ItemStack item) {
        itemType = item.getType();
        amount = item.getAmount();
        itemLore = item.getLore();
        enchantments.addAll(item.getEnchantments());
    }

    @Override
    public ItemStackBuilder itemType(@Nonnull ItemType itemType) {
        Preconditions.checkNotNull(itemType, "itemType");
        this.itemType = itemType;
        this.data = null; // If ItemType changed, we can't use the same data.
        return this;
    }

    @Override
    public ItemStackBuilder amount(int amount) {
        Preconditions.checkState(itemType != null, "ItemType has not been set");
        Preconditions.checkArgument(amount >= 0 && amount <= itemType.getMaximumStackSize(), "Amount %s is not between 0 and %s", amount, itemType.getMaximumStackSize());
        this.amount = amount;
        return this;
    }

    @Override
    public ItemStackBuilder name(@Nonnull String itemName) {
        Preconditions.checkNotNull(itemName, "name");
        this.itemName = itemName;
        return this;
    }

    @Override
    public ItemStackBuilder clearName() {
        this.itemName = null;
        return this;
    }

    @Override
    public ItemStackBuilder lore(List<String> lines) {
        Preconditions.checkNotNull(lines, "lines");
        this.itemLore = ImmutableList.copyOf(lines);
        return this;
    }

    @Override
    public ItemStackBuilder clearLore() {
        this.itemLore = null;
        return this;
    }

    @Override
    public ItemStackBuilder itemData(Object data) {
        if (data != null) {
            Preconditions.checkState(itemType != null, "ItemType has not been set");
            Preconditions.checkArgument(itemType.getMetadataClass() != null, "Item does not have any data associated with it.");
            Preconditions.checkArgument(data.getClass().isAssignableFrom(itemType.getMetadataClass()), "ItemType data is not valid (wanted %s)",
                    itemType.getMetadataClass().getName());
        }
        this.data = data;
        return this;
    }

    @Override
    public ItemStackBuilder addEnchantment(Enchantment enchantment) {
        addEnchantment0(enchantment);
        return this;
    }

    @Override
    public ItemStackBuilder addEnchantments(Collection<Enchantment> enchantments) {
        Preconditions.checkNotNull(enchantments, "enchantments");
        enchantments.forEach(this::addEnchantment0);
        return this;
    }

    @Override
    public ItemStackBuilder clearEnchantments() {
        enchantments.clear();
        return this;
    }

    @Override
    public ItemStackBuilder removeEnchantment(Enchantment enchantment) {
        enchantments.remove(enchantment);
        return this;
    }

    @Override
    public ItemStackBuilder removeEnchantments(Collection<Enchantment> enchantments) {
        Preconditions.checkNotNull(enchantments, "enchantments");
        this.enchantments.removeAll(enchantments);
        return this;
    }

    public ItemStackBuilder nbt(NbtMap nbt) {
        this.nbt = nbt;
        return this;
    }

    public ItemStackBuilder networkData(ItemData data) {
        this.networkData = data;
        return this;
    }

    @Override
    public ItemStack build() {
        Preconditions.checkArgument(itemType != null, "ItemType has not been set");
        return new CloudItemStack(itemType, amount, itemName, itemLore, enchantments, canDestroy, canPlaceOn, nbt, networkData);
    }

    private void addEnchantment0(Enchantment enchantment) {
        Preconditions.checkNotNull(enchantment, "enchantment");
        for (Enchantment ench : enchantments) {
            if (ench.getId() == enchantment.getId()) {
                throw new IllegalArgumentException("Enchantment type is already in builder");
            }
        }
        enchantments.add(enchantment);
    }
}
