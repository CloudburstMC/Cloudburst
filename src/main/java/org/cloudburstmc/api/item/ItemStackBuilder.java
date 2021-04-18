package org.cloudburstmc.api.item;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.util.Identifier;

import java.util.Collection;
import java.util.List;

public interface ItemStackBuilder {

    ItemStackBuilder itemType(@NonNull ItemType itemType);

    ItemStackBuilder blockState(BlockState blockState);

    ItemStackBuilder amount(int amount);

    ItemStackBuilder name(@NonNull String name);

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
