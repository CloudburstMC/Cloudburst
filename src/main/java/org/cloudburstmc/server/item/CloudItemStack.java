package org.cloudburstmc.server.item;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import lombok.ToString;
import lombok.val;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentType;
import org.cloudburstmc.server.item.behavior.ItemBehavior;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.ParametersAreNullableByDefault;
import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ToString
@Immutable
@ParametersAreNonnullByDefault
public class CloudItemStack implements ItemStack {

    protected static final Object NONE_VALUE = new Object();

    protected volatile Identifier id;
    protected final ItemType type;
    protected final int amount;
    protected final String itemName;
    protected final List<String> itemLore;
    protected final ImmutableMap<EnchantmentType, EnchantmentInstance> enchantments;
    protected final Set<Identifier> canDestroy;
    protected final Set<Identifier> canPlaceOn;
    protected final Map<Class<?>, Object> data;

    protected volatile NbtMap nbt;
    protected volatile NbtMap dataTag;
    protected volatile ItemData networkData;

    public CloudItemStack(Identifier id, ItemType type) {
        this(id, type, 1, null, null, null, null, null, null, null, null, null);
    }

    public CloudItemStack(Identifier id, ItemType type, int amount) {
        this(id, type, amount, null, null, null, null, null, null, null, null, null);
    }

    @ParametersAreNullableByDefault
    public CloudItemStack(
            Identifier id,
            ItemType type,
            int amount,
            String itemName,
            List<String> itemLore,
            Map<EnchantmentType, EnchantmentInstance> enchantments,
            Collection<Identifier> canDestroy,
            Collection<Identifier> canPlaceOn,
            Map<Class<?>, Object> data,
            NbtMap nbt,
            NbtMap dataTag,
            ItemData networkData
    ) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.itemName = itemName;
        this.itemLore = itemLore == null ? ImmutableList.of() : ImmutableList.copyOf(itemLore);
        this.enchantments = enchantments == null ? ImmutableMap.of() : ImmutableMap.copyOf(enchantments);
        this.canDestroy = canDestroy == null ? ImmutableSet.of() : ImmutableSet.copyOf(canDestroy);
        this.canPlaceOn = canPlaceOn == null ? ImmutableSet.of() : ImmutableSet.copyOf(canPlaceOn);
        this.data = data == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(data);
        this.nbt = nbt;
        this.dataTag = dataTag;
        this.networkData = networkData;
    }

    public Identifier getId() {
        if (id == null) {
            synchronized (type) {
                if (id == null) {
                    id = Identifier.fromString(getNbt().getString("Name"));
                }
            }
        }

        return id;
    }

    @Override
    public ItemType getType() {
        return type;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getMetadata(Class<T> metadataClass, T defaultValue) {
        T value = (T) this.data.get(metadataClass);

        if (value == null) {
            val serializer = CloudItemRegistry.get().getSerializer(metadataClass);
            if (serializer != null) {
                value = (T) serializer.deserialize(this.id, getNbt(), getDataTag());
            }

            if (value == null) {
                value = (T) NONE_VALUE;
            }

            this.data.put(metadataClass, value);
        }

        if (value == NONE_VALUE) {
            return defaultValue;
        }

        return value;
    }

    public ImmutableMap<Class<?>, Object> getData() {
        return ImmutableMap.copyOf(data);
    }

    @Override
    public String getName() {
        return itemName;
    }

    @Override
    public List<String> getLore() {
        return itemLore;
    }

    @Override
    public Map<EnchantmentType, EnchantmentInstance> getEnchantments() {
        return enchantments;
    }

    @Override
    public Collection<Identifier> getCanDestroy() {
        return this.canDestroy;
    }

    @Override
    public Collection<Identifier> getCanPlaceOn() {
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

    public NbtMap getNbt() {
        if (nbt == null) {
            synchronized (itemLore) {
                if (nbt == null) {
                    NbtMapBuilder builder = NbtMap.builder();
                    CloudItemRegistry.get().getSerializer(this.type).serialize(this, builder);
                    this.nbt = builder.build();
                }
            }
        }

        return this.nbt;
    }

    public NbtMap getDataTag() {
        if (dataTag == null) {
            getNbt();
            if (nbt.containsKey("tag")) {
                dataTag = nbt.getCompound("tag");
            } else {
                dataTag = NbtMap.EMPTY;
            }
        }

        return dataTag;
    }

    public ItemData getNetworkData() {
        if (networkData == null) {
            synchronized (this) {
                if (networkData == null) {
                    this.networkData = ItemUtils.toNetwork(this);
                }
            }
        }
        return networkData;
    }

    @Override
    public boolean hasTag() {
        if (this.nbt != null) {
            val tag = this.nbt.getCompound("tag");
            if (tag != null && !tag.isEmpty()) {
                return true;
            }
        }

        return !(this.data.isEmpty() || this.itemLore.isEmpty() || this.itemName == null || this.enchantments.isEmpty() || this.canPlaceOn.isEmpty() || this.canDestroy.isEmpty());
    }

    @Override
    public ItemStack withData(Object data) {
        Preconditions.checkNotNull(data, "data");
        return withData(data.getClass(), data);
    }

    @Override
    public ItemStack withData(Class<?> metadataClass, Object data) {
        return this.toBuilder().itemData(metadataClass, data).build();
    }

    @Override
    public ItemBehavior getBehavior() {
        return CloudItemRegistry.get().getBehavior(this.type);
    }

    @Override
    public boolean isMergeable(@Nonnull ItemStack other) {
        return equals(other);
    }

    @Override
    public boolean equals(@Nullable ItemStack item) {
        return equals(item, false, hasTag());
    }

    @Override
    public boolean equals(@Nullable ItemStack other, boolean checkAmount, boolean checkData) {
        if (this == other) return true;
        if (other == null) return false;
        CloudItemStack that = (CloudItemStack) other;

        return this.type == that.type && (!checkAmount || this.amount == that.amount) &&
                (!checkData || Objects.equals(this.getDataTag(), that.getDataTag())); //TODO: damage
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
