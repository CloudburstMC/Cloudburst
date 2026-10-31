package org.cloudburstmc.server.container;

import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainerFactory;

/**
 * Provides {@link CloudContainer} as the storage behind detached item containers.
 */
public final class CloudContainerFactory implements ItemContainerFactory {

    @Override
    public ItemContainer create(int size) {
        return new CloudContainer(size);
    }
}
