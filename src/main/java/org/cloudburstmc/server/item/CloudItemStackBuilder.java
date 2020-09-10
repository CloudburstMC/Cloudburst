package org.cloudburstmc.server.item;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.ToString;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Nonnull
@ToString
public class CloudItemStackBuilder implements ItemStackBuilder {

    private Identifier id;
    private ItemType itemType;
    private int amount = 1;
    private Reference2ObjectMap<Class<?>, Object> data = new Reference2ObjectOpenHashMap<>();
    private String itemName;
    private List<String> itemLore;
    private final Set<EnchantmentInstance> enchantments = new HashSet<>();
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

    public CloudItemStackBuilder id(@Nonnull Identifier id) {
        Preconditions.checkNotNull(id, "id");
        this.id = id;
        return this;
    }

    @Override
    public CloudItemStackBuilder itemType(@Nonnull ItemType itemType) {
        Preconditions.checkNotNull(itemType, "itemType");
        this.itemType = itemType;
        this.data = new Reference2ObjectOpenHashMap<>(); // If ItemType changed, we can't use the same data.
        return this;
    }

    @Override
    public CloudItemStackBuilder amount(int amount) {
        Preconditions.checkState(itemType != null, "ItemType has not been set");
        Preconditions.checkArgument(amount >= 0 && amount <= itemType.getMaximumStackSize(), "Amount %s is not between 0 and %s", amount, itemType.getMaximumStackSize());
        this.amount = amount;
        return this;
    }

    @Override
    public CloudItemStackBuilder name(@Nonnull String itemName) {
        Preconditions.checkNotNull(itemName, "name");
        this.itemName = itemName;
        return this;
    }

    @Override
    public CloudItemStackBuilder clearName() {
        this.itemName = null;
        return this;
    }

    @Override
    public CloudItemStackBuilder lore(List<String> lines) {
        Preconditions.checkNotNull(lines, "lines");
        this.itemLore = ImmutableList.copyOf(lines);
        return this;
    }

    @Override
    public CloudItemStackBuilder clearLore() {
        this.itemLore = null;
        return this;
    }

    @Override
    public CloudItemStackBuilder itemData(Object data) {
        Preconditions.checkState(itemType != null, "ItemType has not been set");
        this.data.put(data.getClass(), data);
        return this;
    }

    public CloudItemStackBuilder itemData(Object... data) {
        Preconditions.checkState(itemType != null, "ItemType has not been set");

        for (Object d : data) {
            this.data.put(d.getClass(), d);
        }
        return this;
    }

    @Override
    public CloudItemStackBuilder addEnchantment(EnchantmentInstance enchantment) {
        addEnchantment0(enchantment);
        return this;
    }

    @Override
    public CloudItemStackBuilder addEnchantments(Collection<EnchantmentInstance> enchantments) {
        Preconditions.checkNotNull(enchantments, "enchantments");
        enchantments.forEach(this::addEnchantment0);
        return this;
    }

    @Override
    public CloudItemStackBuilder clearEnchantments() {
        enchantments.clear();
        return this;
    }

    @Override
    public CloudItemStackBuilder removeEnchantment(EnchantmentInstance enchantment) {
        enchantments.remove(enchantment);
        return this;
    }

    @Override
    public CloudItemStackBuilder removeEnchantments(Collection<EnchantmentInstance> enchantments) {
        Preconditions.checkNotNull(enchantments, "enchantments");
        this.enchantments.removeAll(enchantments);
        return this;
    }

    public CloudItemStackBuilder nbt(NbtMap nbt) {
        this.nbt = nbt;
        return this;
    }

    public CloudItemStackBuilder networkData(ItemData data) {
        this.networkData = data;
        return this;
    }

    @Override
    public CloudItemStack build() {
        Preconditions.checkArgument(itemType != null, "ItemType has not been set");
        return new CloudItemStack(id, itemType, amount, itemName, itemLore, enchantments, canDestroy, canPlaceOn, data, nbt, networkData);
    }

    private void addEnchantment0(EnchantmentInstance enchantment) {
        Preconditions.checkNotNull(enchantment, "enchantment");
        for (EnchantmentInstance ench : enchantments) {
            if (ench.getId() == enchantment.getId()) {
                throw new IllegalArgumentException("Enchantment type is already in builder");
            }
        }
        enchantments.add(enchantment);
    }
}
