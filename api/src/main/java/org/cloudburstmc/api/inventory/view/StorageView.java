package org.cloudburstmc.api.inventory.view;

/**
 * Represents the item slots of a generic storage container
 * (chest, barrel, or shulker box).
 *
 * <p>Real block-entity-backed containers implement {@link BlockStorageView}, which also
 * extends {@link BlockSlotGroup}. Virtual containers (with no backing
 * block entity) implement only this interface.</p>
 */
public interface StorageView extends SlotGroup {
}
