package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

/**
 * Fired before a requested change to gliding. Cancelling leaves the state unchanged.
 */
public class PlayerToggleGlideEvent extends PlayerEvent implements Cancellable {

    private final boolean gliding;

    /**
     * Creates an event for a requested change to gliding.
     *
     * @param player  the player whose gliding state would change
     * @param gliding the requested gliding state
     */
    public PlayerToggleGlideEvent(Player player, boolean gliding) {
        super(player);
        this.gliding = gliding;
    }

    /**
     * Returns the requested gliding state, not the player's current state.
     */
    public boolean isGliding() {
        return this.gliding;
    }

}
