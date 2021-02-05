package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

public class PlayerToggleFlightEvent extends PlayerEvent implements Cancellable {

    protected final boolean isFlying;

    public PlayerToggleFlightEvent(Player player, boolean isFlying) {
        super(player);
        this.isFlying = isFlying;
    }

    public boolean isFlying() {
        return this.isFlying;
    }
}
