package org.cloudburstmc.api.registry;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTag;
import org.cloudburstmc.api.item.ItemTagKey;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.component.ComponentBuilder;
import org.cloudburstmc.api.util.component.ComponentMap;

import java.util.Collection;

/**
 * Registry for item types, item behaviors, creative inventory entries, and item tags.
 */
public interface ItemRegistry extends ComponentRegistry<ItemType>, KeyedRegistry<ItemType> {

    /**
     * Adds a nonempty item stack to the creative inventory listing during initialization.
     *
     * @param item creative inventory entry
     * @throws RegistryException if the item type is unknown, the stack is empty or registration has closed
     */
    void registerCreativeItem(ItemStack item);

    /**
     * Returns the resolved component map for an item type.
     *
     * @param type item type
     * @return resolved component map
     * @throws RegistryException if the item type is unknown
     */
    @Override
    @NonNull
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

    /**
     * Returns a builder for configuring a registered item type during initialization.
     *
     * @param type registered item type
     * @return component builder for the type
     * @throws RegistryException if the type is unknown or registration has closed
     */
    ComponentBuilder configure(ItemType type) throws RegistryException;
}
