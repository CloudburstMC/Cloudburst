package org.cloudburstmc.server.item;

import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentType;
import org.cloudburstmc.server.utils.Identifier;

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

    ItemStackBuilder itemData(Class<?> metadataClass, Object data);

    ItemStackBuilder clearData();

    ItemStackBuilder clearData(Class<?> metadataClass);

    ItemStackBuilder addEnchantment(EnchantmentInstance enchantment);

    ItemStackBuilder addEnchantments(Collection<EnchantmentInstance> enchantmentInstanceCollection);

    ItemStackBuilder clearEnchantments();

    ItemStackBuilder removeEnchantment(EnchantmentType enchantment);

    ItemStackBuilder removeEnchantments(Collection<EnchantmentType> enchantments);

    ItemStackBuilder addCanPlaceOn(Identifier id);

    ItemStackBuilder addCanPlaceOn(ItemType type);

    ItemStackBuilder removeCanPlaceOn(Identifier id);

    ItemStackBuilder clearCanPlaceOn();

    ItemStackBuilder addCanDestroy(Identifier id);

    ItemStackBuilder addCanDestroy(ItemType type);

    ItemStackBuilder removeCanDestroy(Identifier id);

    ItemStackBuilder clearCanDestroy();

    ItemStack build();
}
