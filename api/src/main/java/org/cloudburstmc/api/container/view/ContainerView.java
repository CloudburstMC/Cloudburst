package org.cloudburstmc.api.container.view;

import org.cloudburstmc.api.container.Container;
import org.cloudburstmc.api.container.ContainerViewType;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents a view of a container.
 */
public interface ContainerView {

    ContainerViewType<? extends ContainerView> getViewType();

    /**
     * Get the underlying container backing this view.
     *
     * @return the container
     */
    Container getContainer();

    ItemStack getItem(int slot);

    void setItem(int slot, ItemStack itemStack);

    int size();
}
