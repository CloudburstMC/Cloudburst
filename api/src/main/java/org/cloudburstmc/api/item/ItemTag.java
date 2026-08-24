package org.cloudburstmc.api.item;

import java.util.Set;

/**
 * A named, read-only set of item types.
 */
public interface ItemTag {

    /**
     * @return the key used to identify this tag
     */
    ItemTagKey getKey();

    /**
     * @param type item type to test
     * @return whether {@code type} is a member
     */
    boolean isTagged(ItemType type);

    /**
     * @return all members in an immutable set
     */
    Set<ItemType> getValues();
}
