package org.cloudburstmc.api.inventory;

/**
 * Builds the storage behind detached item containers.
 *
 * <p>The server provides exactly one implementation through {@link java.util.ServiceLoader}
 * ({@code META-INF/services/org.cloudburstmc.api.inventory.ItemContainerFactory});
 * {@link ItemContainers#detached(int)} discovers it on first use.</p>
 */
public interface ItemContainerFactory {

    /**
     * Creates an empty container.
     *
     * @param size the slot count
     * @return a new empty container with the given slot count
     */
    ItemContainer create(int size);
}
