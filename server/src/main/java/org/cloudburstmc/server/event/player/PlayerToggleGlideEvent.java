package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.player.Player;

public class PlayerToggleGlideEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    protected final boolean isGliding;

    public PlayerToggleGlideEvent(Player player, boolean isSneaking) {
        super(player);
        this.isGliding = isSneaking;
    }

    public boolean isGliding() {
        return this.isGliding;
    }

}
