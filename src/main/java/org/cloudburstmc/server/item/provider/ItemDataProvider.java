package org.cloudburstmc.server.item.provider;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import lombok.val;
import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.registry.EnchantmentRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ItemDataProvider {

    protected final CloudItemStack item;
    protected final NbtMap tag;

    public ItemDataProvider(CloudItemStack item, NbtMap tag) {
        this.item = item;
        this.tag = tag;
    }

    public String getCustomName() {
        val display = tag.getCompound("display");

        if (display != null) {
            return tag.getString("Name");
        }

        return null;
    }

    public List<String> getLore() {
        val display = tag.getCompound("display");

        if (display != null) {
            return tag.getList("Lore", NbtType.STRING);
        }

        return null;
    }

    public Set<EnchantmentInstance> getEnchantments() {
        Set<EnchantmentInstance> enchantments = new HashSet<>();
        val registry = EnchantmentRegistry.get();

        tag.listenForList("ench", NbtType.COMPOUND, tags -> {
            for (NbtMap entry : tags) {
                short id = entry.getShort("id");
                int level = entry.getShort("lvl");
                enchantments.add(registry.getEnchantment(registry.getType(id), level));
            }
        });

        return enchantments;
    }

    public abstract Set<Identifier> getCanDestroy();

    public abstract Set<Identifier> getCanPlaceOn();

    public <T> T getMetadata(Class<T> clazz) {
        return null; //TODO: deserialize
    }
}
