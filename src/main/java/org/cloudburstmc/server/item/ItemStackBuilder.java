package org.cloudburstmc.server.item;

import org.cloudburstmc.server.enchantment.EnchantmentInstance;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public interface ItemStackBuilder {

    ItemStackBuilder itemType(@Nonnull ItemType itemType);

    ItemStackBuilder amount(int amount);

    ItemStackBuilder name(@Nonnull String name);

    ItemStackBuilder clearName();

    ItemStackBuilder lore(List<String> lines);

    ItemStackBuilder clearLore();

    ItemStackBuilder itemData(Object data);

    ItemStackBuilder addEnchantment(EnchantmentInstance enchantment);

    ItemStackBuilder addEnchantments(Collection<EnchantmentInstance> enchantmentInstanceCollection);

    ItemStackBuilder clearEnchantments();

    ItemStackBuilder removeEnchantment(EnchantmentInstance enchantment);

    ItemStackBuilder removeEnchantments(Collection<EnchantmentInstance> enchantments);

    ItemStack build();
}
