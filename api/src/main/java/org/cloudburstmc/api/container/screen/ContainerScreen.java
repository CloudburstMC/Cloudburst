package org.cloudburstmc.api.container.screen;

import org.cloudburstmc.api.container.ContainerScreenType;
import org.cloudburstmc.api.container.ContainerViewType;
import org.cloudburstmc.api.container.view.ContainerView;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public interface ContainerScreen {

    /**
     * Retrieves the type of this screen.
     *
     * @return the type
     */
    ContainerScreenType<?> getType();

    /**
     * Retrieves the backing inventories for this screen.
     *
     * @return the backing inventories
     */
    Set<ContainerViewType<?>> getViewTypes();

    /**
     * Retrieves a view that is used in this screen.
     *
     * @param type the type of view
     * @param <T>  the type of view
     * @return the view
     */
    <T extends ContainerView> Optional<T> getView(ContainerViewType<T> type);

    default <T extends ContainerView> T getViewOrThrow(ContainerViewType<T> type) {
        return getView(type).orElseThrow(() -> new NoSuchElementException("View type " + type + " is not present in this screen"));
    }

    Collection<ContainerView> getViews();
}
