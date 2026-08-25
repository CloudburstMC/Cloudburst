package org.cloudburstmc.api.registry;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTag;
import org.cloudburstmc.api.item.ItemTagKey;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.component.ComponentMap;

import java.util.Collection;

public interface ItemRegistry extends ComponentRegistry<ItemType> {

    void registerCreativeItem(ItemStack item);

    @Override
    ComponentMap getComponents(ItemType type);

    Identifier getIdentifier(int runtimeId) throws RegistryException;

    ItemType getType(Identifier runtimeId, int data);

    ItemType getType(int runtimeId, int data);

    /**
     * Returns the item tag identified by a key.
     *
     * @param key tag key to resolve
     * @return matching read-only tag
     * @throws IllegalArgumentException if the key is unknown
     */
    ItemTag getTag(ItemTagKey key);

    /**
     * Returns all item tags known to this registry.
     *
     * @return immutable collection of item tags
     */
    Collection<ItemTag> getTags();

    ImmutableList<Identifier> getItems();

}
