package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.player.Player;

/**
 * A {@link SlotGroup} associated with a specific player.
 */
public interface PlayerSlotGroup extends EntitySlotGroup {

    /**
     * Returns the player that owns this slot group.
     *
     * @return the owning player
     */
    @Override
    Player getHolder();
}
