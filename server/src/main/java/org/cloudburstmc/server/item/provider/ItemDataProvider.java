package org.cloudburstmc.server.item.provider;

import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.registry.EnchantmentRegistry;

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
        var display = tag.getCompound("display");

        if (display != null) {
            return tag.getString("Name");
        }

        return null;
    }

    public List<String> getLore() {
        var display = tag.getCompound("display");

        if (display != null) {
            return tag.getList("Lore", NbtType.STRING);
        }

        return null;
    }

    public Set<EnchantmentInstance> getEnchantments() {
        Set<EnchantmentInstance> enchantments = new HashSet<>();
        var registry = EnchantmentRegistry.get();

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
