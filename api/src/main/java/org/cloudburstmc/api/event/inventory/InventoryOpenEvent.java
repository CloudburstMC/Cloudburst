package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.container.view.ContainerView;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

/**
 * author: Box
 * Nukkit Project
 */
public final class InventoryOpenEvent extends InventoryEvent implements Cancellable {

    private final Player who;

    public InventoryOpenEvent(ContainerView inventory, Player who) {
        super(inventory);
        this.who = who;
    }

    public Player getPlayer() {
        return this.who;
    }
}
