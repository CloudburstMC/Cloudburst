package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.StorageView;

/**
 * Common ancestor for screens that expose a {@link StorageView}.
 *
 * <p>Two concrete subtypes exist:</p>
 * <ul>
 *   <li>{@link StorageScreen}: a real container backed by a block entity in the world;
 *       its {@link #getStorage()} narrows the return type to {@link org.cloudburstmc.api.inventory.view.BlockStorageView}.</li>
 *   <li>{@link VirtualStorageScreen}: a plugin-defined container with no block entity;
 *       its {@link #getStorage()} returns a plain {@link StorageView}.</li>
 * </ul>
 *
 * <p>Write code against this interface when you want to operate on storage slots without
 * caring whether the container is backed by a block entity:</p>
 * <pre>{@code
 * void fillWith(StorageAccess access, ItemStack item) {
 *     StorageView storage = access.getStorage();
 *     for (int i = 0; i < storage.getSize(); i++) {
 *         storage.setItem(i, item);
 *     }
 * }
 * }</pre>
 */
public interface StorageAccess {

    /**
     * Returns the storage section of this container.
     *
     * @return the storage view (never {@code null})
     */
    StorageView getStorage();
}
