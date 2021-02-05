package org.cloudburstmc.server.inventory;

import org.cloudburstmc.server.player.CloudPlayer;

/**
 * @author CreeperFace
 */
public class PlayerCursorInventory extends PlayerUIComponent {
    private final PlayerUIInventory playerUI;

    PlayerCursorInventory(PlayerUIInventory playerUI) {
        super(playerUI, 0, 1);
        this.playerUI = playerUI;
    }

    /**
     * This override is here for documentation and code completion purposes only.
     *
     * @return Player
     */
    @Override
    public CloudPlayer getHolder() {
        return playerUI.getHolder();
    }
}
