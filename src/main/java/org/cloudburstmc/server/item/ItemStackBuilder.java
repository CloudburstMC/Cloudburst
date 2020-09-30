package org.cloudburstmc.server.item;

import org.cloudburstmc.server.item.enchantment.Enchantment;

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

    ItemStackBuilder addEnchantment(Enchantment enchantment);

    ItemStackBuilder addEnchantments(Collection<Enchantment> enchantmentInstanceCollection);

    ItemStackBuilder clearEnchantments();

    ItemStackBuilder removeEnchantment(Enchantment enchantment);

    ItemStackBuilder removeEnchantments(Collection<Enchantment> enchantments);

    ItemStack build();
}
