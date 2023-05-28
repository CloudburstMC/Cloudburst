package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

public final class PlayerToggleSprintEvent extends PlayerEvent implements Cancellable {

    protected final boolean isSprinting;

    public PlayerToggleSprintEvent(Player player, boolean isSprinting) {
        super(player);
        this.isSprinting = isSprinting;
    }

    public boolean isSprinting() {
        return this.isSprinting;
    }

}
