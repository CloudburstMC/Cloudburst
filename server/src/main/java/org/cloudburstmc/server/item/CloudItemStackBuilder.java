package org.cloudburstmc.server.item;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.ToString;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentType;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@Nonnull
@ToString
@ParametersAreNonnullByDefault
public class CloudItemStackBuilder implements ItemStackBuilder {

    private Identifier id;
    private ItemType itemType;
    private int amount = 1;
    private Reference2ObjectMap<Class<?>, Object> data = new Reference2ObjectOpenHashMap<>();
    private String itemName;
    private List<String> itemLore;
    private final Map<EnchantmentType, EnchantmentInstance> enchantments = new Reference2ObjectOpenHashMap<>();
    private final Set<Identifier> canDestroy = new HashSet<>();
    private final Set<Identifier> canPlaceOn = new HashSet<>();
    private NbtMap nbt;
    private NbtMap dataTag;
    private ItemData networkData;

    public CloudItemStackBuilder() {
    }

    public CloudItemStackBuilder(ItemStack item) {
        itemType = item.getType();
        amount = item.getAmount();
        itemLore = item.getLore();
        enchantments.putAll(item.getEnchantments());
        canDestroy.addAll(item.getCanDestroy());
        canPlaceOn.addAll(item.getCanPlaceOn());
    }

    public CloudItemStackBuilder id(Identifier id) {
        Preconditions.checkNotNull(id, "id");
        this.id = id;
        return this;
    }

    @Override
    public CloudItemStackBuilder itemType(ItemType itemType) {
        Preconditions.checkNotNull(itemType, "itemType");
        this.itemType = itemType;
//        this.data = new Reference2ObjectOpenHashMap<>(); // If ItemType changed, we can't use the same data.
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
    public CloudItemStackBuilder name(String itemName) {
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
        this.data.put(data.getClass(), data);
        return this;
    }

    public CloudItemStackBuilder itemData(Object... data) {
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
    public CloudItemStackBuilder removeEnchantment(EnchantmentType enchantment) {
        enchantments.remove(enchantment);
        return this;
    }

    @Override
    public CloudItemStackBuilder removeEnchantments(Collection<EnchantmentType> enchantments) {
        Preconditions.checkNotNull(enchantments, "enchantments");
        enchantments.forEach(this::removeEnchantment);
        return this;
    }

    public CloudItemStackBuilder nbt(NbtMap nbt) {
        this.nbt = nbt;
        return this;
    }

    public CloudItemStackBuilder dataTag(NbtMap nbt) {
        this.dataTag = nbt;
        return this;
    }

    public CloudItemStackBuilder networkData(ItemData data) {
        this.networkData = data;
        return this;
    }

    @Override
    public ItemStackBuilder itemData(Class<?> metadataClass, Object data) {
        Preconditions.checkNotNull(data, "data");
        Preconditions.checkNotNull(metadataClass, "metadataClass");
        Preconditions.checkArgument(metadataClass.isAssignableFrom(data.getClass()), "data must be an instance of metadataClass");
        this.data.put(metadataClass, data);
        return this;
    }

    @Override
    public ItemStackBuilder clearData() {
        this.data.clear();
        return this;
    }

    @Override
    public ItemStackBuilder clearData(Class<?> metadataClass) {
        this.data.remove(metadataClass);
        return this;
    }

    @Override
    public ItemStackBuilder addCanPlaceOn(Identifier id) {
        Preconditions.checkNotNull(id, "id");
        this.canPlaceOn.add(id);
        return this;
    }

    @Override
    public ItemStackBuilder addCanPlaceOn(ItemType type) {
        Preconditions.checkNotNull(type, "type");
        this.canPlaceOn.addAll(CloudItemRegistry.get().getIdentifiers(type));
        return this;
    }

    @Override
    public ItemStackBuilder removeCanPlaceOn(Identifier id) {
        Preconditions.checkNotNull(id, "id");
        this.canPlaceOn.remove(id);
        return this;
    }

    @Override
    public ItemStackBuilder clearCanPlaceOn() {
        this.canPlaceOn.clear();
        return this;
    }

    @Override
    public ItemStackBuilder addCanDestroy(Identifier id) {
        Preconditions.checkNotNull(id, "id");
        this.canDestroy.add(id);
        return this;
    }

    @Override
    public ItemStackBuilder addCanDestroy(ItemType type) {
        Preconditions.checkNotNull(type, "type");
        this.canDestroy.addAll(CloudItemRegistry.get().getIdentifiers(type));
        return this;
    }

    @Override
    public ItemStackBuilder removeCanDestroy(Identifier id) {
        Preconditions.checkNotNull(id, "id");
        this.canDestroy.remove(id);
        return this;
    }

    @Override
    public ItemStackBuilder clearCanDestroy() {
        this.canDestroy.clear();
        return this;
    }

    @Override
    public CloudItemStack build() {
        Preconditions.checkArgument(itemType != null, "ItemType has not been set");
        return new CloudItemStack(id, itemType, amount, itemName, itemLore, enchantments, canDestroy, canPlaceOn, data, nbt, networkData);
    }

    private void addEnchantment0(EnchantmentInstance enchantment) {
        Preconditions.checkNotNull(enchantment, "enchantment");
        Preconditions.checkArgument(!this.enchantments.containsKey(enchantment.getType()), "Enchantment type is already in builder");

        enchantments.put(enchantment.getType(), enchantment);
    }
}
