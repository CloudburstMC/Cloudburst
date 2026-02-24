package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.BlockStorageView;

/**
 * Represents an open storage container screen
 * (chest, barrel, or shulker box).
 *
 * <p>Extends {@link StorageAccess} so that code written against the common interface
 * can operate on either a block-backed or virtual storage container without knowing which
 * concrete type it holds. The return type of {@link #getStorage()} is narrowed here to
 * {@link BlockStorageView}, which additionally exposes {@link org.cloudburstmc.api.inventory.view.BlockSlotGroup}
 * methods (block position, block entity reference, etc.).</p>
 */
public interface StorageScreen extends ContainerScreen, StorageAccess {

    /**
     * Returns the storage section of this container.
     *
     * @return the block-backed storage section (never {@code null})
     */
    @Override
    BlockStorageView getStorage();
}
