package org.cloudburstmc.api.registry;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.behavior.ItemBehavior;
import org.cloudburstmc.api.util.Identifier;

public interface ItemRegistry extends BehaviorRegistry {

    void register(ItemType type, ItemBehavior behavior, Identifier... identifiers) throws RegistryException;

    void registerCreativeItem(ItemStack item);

    Identifier getIdentifier(int runtimeId) throws RegistryException;

    ImmutableList<Identifier> getItems();

}
