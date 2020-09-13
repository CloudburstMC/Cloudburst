package org.cloudburstmc.server.registry;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemType;
import org.cloudburstmc.server.item.behavior.ItemBehavior;
import org.cloudburstmc.server.item.serializer.ItemSerializer;
import org.cloudburstmc.server.utils.Identifier;

public interface ItemRegistry extends Registry {

    void register(ItemType type, ItemSerializer serializer, ItemBehavior behavior, Identifier... identifiers) throws RegistryException;

    ItemStack getItem(BlockState state) throws RegistryException;

    ItemStack getItem(ItemType type, Object... metadata) throws RegistryException;

    Identifier getIdentifier(int runtimeId) throws RegistryException;

    ImmutableList<Identifier> getItems();

//    <T> ItemDataSerializer<T> getSerializer(Class<T> metaClass);
}
