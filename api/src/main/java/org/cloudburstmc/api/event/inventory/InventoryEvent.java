package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.inventory.InventoryScreen;
import org.cloudburstmc.api.player.Player;

/**
 * Base event for actions on an open inventory screen.
 *
 * <p>An {@link InventoryScreen} represents the full open window shown to a player (e.g. a chest
 * screen, crafting table screen, or the player's own inventory screen). Events that relate to
 * block-level container operations that do not involve an open screen (e.g. hoppers moving items,
 * players picking up dropped items) extend {@link Event} directly rather than this class.</p>
 */
public abstract class InventoryEvent extends Event {

    protected final InventoryScreen screen;

    public InventoryEvent(InventoryScreen screen) {
        this.screen = screen;
    }

    /**
     * Returns the inventory screen (open window) that this event is about.
     *
     * @return the inventory screen
     */
    public InventoryScreen getScreen() {
        return screen;
    }

    /**
     * Returns the player who has this inventory screen open.
     *
     * @return the player
     */
    public Player getPlayer() {
        return screen.getPlayer();
    }
}
