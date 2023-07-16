package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.container.view.ContainerView;
import org.cloudburstmc.api.event.Event;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class InventoryEvent extends Event {

    protected final ContainerView inventory;

    public InventoryEvent(ContainerView inventory) {
        this.inventory = inventory;
    }

    public ContainerView getView() {
        return inventory;
    }

}
