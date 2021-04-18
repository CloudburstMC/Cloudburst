package org.cloudburstmc.server.item;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.ToString;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@Nonnull
@ToString
@ParametersAreNonnullByDefault
public class CloudItemStackBuilder implements ItemStackBuilder {

    private Identifier id;
    private ItemType itemType;
    private BlockState blockState;
    private int amount = 1;
    private final Reference2ObjectMap<Class<?>, Object> data = new Reference2ObjectOpenHashMap<>();
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

        if (item instanceof BlockItemStack) {
            this.blockState = item.getBlockState();
        }
    }

    public CloudItemStackBuilder id(Identifier id) {
        Preconditions.checkState(this.blockState == null || id == this.blockState.getType().getId(), "Cannot change item id when block state is set");
        this.id = id;
        return this;
    }

    @Override
    public CloudItemStackBuilder itemType(ItemType itemType) {
        Preconditions.checkNotNull(itemType, "itemType");
        Preconditions.checkState(this.blockState == null || itemType == this.blockState.getType(), "Cannot change item id when block state is set");
        this.itemType = itemType;
        if (itemType instanceof BlockType) {
            this.blockState = CloudBlockRegistry.get().getBlock((BlockType) itemType);
        } else {
            this.blockState = null;
        }
        return this;
    }

    @Override
    public CloudItemStackBuilder blockState(BlockState blockState) {
        Preconditions.checkNotNull(blockState, "blockState");
        this.blockState = blockState;
        this.itemType = blockState.getType();
        return this;
    }

    @Override
    public CloudItemStackBuilder amount(int amount) {
        return amount(amount, true);
    }

    public CloudItemStackBuilder amount(int amount, boolean safe) {
        Preconditions.checkState(itemType != null, "ItemType has not been set");
        Preconditions.checkArgument(!safe || (amount >= 0 && amount <= itemType.getMaximumStackSize()), "Amount %s is not between 0 and %s", amount, itemType.getMaximumStackSize());
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

        if (amount <= 0) {
            return CloudItemRegistry.AIR;
        }

        if (blockState != null) {
            if (blockState == BlockStates.AIR) {
                return CloudItemRegistry.AIR;
            }

            return new BlockItemStack(this.blockState, amount, itemName, itemLore, enchantments, canDestroy, canPlaceOn, data, nbt, dataTag, networkData);
        } else {
            return new CloudItemStack(id, itemType, amount, itemName, itemLore, enchantments, canDestroy, canPlaceOn, data, nbt, dataTag, networkData);
        }
    }

    private void addEnchantment0(EnchantmentInstance enchantment) {
        Preconditions.checkNotNull(enchantment, "enchantment");
        Preconditions.checkArgument(!this.enchantments.containsKey(enchantment.getType()), "Enchantment type is already in builder");

        enchantments.put(enchantment.getType(), enchantment);
    }
}
