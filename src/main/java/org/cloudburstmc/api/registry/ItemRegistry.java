package org.cloudburstmc.api.registry;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.behavior.ItemBehavior;
import org.cloudburstmc.api.util.Identifier;

public interface ItemRegistry extends Registry {

    void register(ItemType type, ItemBehavior behavior, Identifier... identifiers) throws RegistryException;

    void registerCreativeItem(ItemStack item);

    default ItemStack getItem(BlockState state) throws RegistryException {
        return getItem(state, 1);
    }

    ItemStack getItem(BlockState state, int amount) throws RegistryException;

    default ItemStack getItem(ItemType type) throws RegistryException {
        return getItem(type, 1);
    }

    ItemStack getItem(ItemType type, int amount, Object... metadata) throws RegistryException;

    Identifier getIdentifier(int runtimeId) throws RegistryException;

    ImmutableList<Identifier> getItems();

}
