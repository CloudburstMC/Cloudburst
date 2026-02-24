package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.StorageView;

/**
 * A virtual container screen backed by a storage grid with no block entity.
 *
 * <p>Extends {@link StorageAccess} so that code written against the common interface can
 * treat real and virtual storage containers uniformly. Concrete subtypes differ only in slot
 * count and the phantom block used:</p>
 * <ul>
 *   <li>{@link VirtualChestScreen} — 27 slots, single chest</li>
 *   <li>{@link VirtualDoubleChestScreen} — 54 slots, double chest</li>
 * </ul>
 */
public interface VirtualStorageScreen extends VirtualContainerScreen, StorageAccess {

    /**
     * Returns the storage section of this virtual container.
     *
     * @return the storage section (never {@code null})
     */
    @Override
    StorageView getStorage();
}
