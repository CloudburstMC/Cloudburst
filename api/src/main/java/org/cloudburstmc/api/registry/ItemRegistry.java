package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTag;
import org.cloudburstmc.api.item.ItemTagKey;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.component.ComponentMap;

import java.util.Collection;

/**
 * Registry for item types, item components, creative inventory entries, and item tags.
 */
public interface ItemRegistry extends ComponentRegistry<ItemType>, KeyedRegistry<ItemType> {

    /**
     * Adds an item stack to the creative inventory listing.
     *
     * @param item creative inventory entry
     */
    void registerCreativeItem(ItemStack item);

    /**
     * Returns the resolved component map for an item type.
     *
     * @param type item type
     * @return resolved component map
     */
    @Override
    ComponentMap getComponents(ItemType type);

    @Override
    default Identifier getId(ItemType value) {
        return value.getId();
    }

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
}
