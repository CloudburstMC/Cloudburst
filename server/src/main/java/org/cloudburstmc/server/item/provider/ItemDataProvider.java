package org.cloudburstmc.server.item.provider;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.registry.CloudEnchantmentRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ItemDataProvider {

    protected final ItemStack item;
    protected final NbtMap tag;

    public ItemDataProvider(ItemStack item, NbtMap tag) {
        this.item = item;
        this.tag = tag;
    }

    public String getCustomName() {
        NbtMap display = tag.getCompound("display");
        if (display != null) {
            return tag.getString("Name");
        }

        return null;
    }

    public List<String> getLore() {
        NbtMap display = tag.getCompound("display");
        if (display != null) {
            return tag.getList("Lore", NbtType.STRING);
        }

        return null;
    }

    public Set<Enchantment> getEnchantments() {
        Set<Enchantment> enchantments = new HashSet<>();
        CloudEnchantmentRegistry registry = CloudEnchantmentRegistry.get();

        tag.listenForList("ench", NbtType.COMPOUND, tags -> {
            for (NbtMap entry : tags) {
                short id = entry.getShort("id");
                int level = entry.getShort("lvl");
                EnchantmentType type = registry.getType(id);
                if (type != null && level > 0) {
                    enchantments.add(registry.getEnchantment(type, level));
                }
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
