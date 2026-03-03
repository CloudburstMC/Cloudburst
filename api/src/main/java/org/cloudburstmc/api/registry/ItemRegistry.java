package org.cloudburstmc.api.registry;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.component.ComponentMap;

public interface ItemRegistry extends ComponentRegistry<ItemType> {

    void registerCreativeItem(ItemStack item);

    @Override
    ComponentMap getComponents(ItemType type);

    Identifier getIdentifier(int runtimeId) throws RegistryException;

    ItemType getType(Identifier runtimeId, int data);

    ItemType getType(int runtimeId, int data);

    ImmutableList<Identifier> getItems();

}
