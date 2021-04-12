package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * @author CreeperFace
 */
public class PlayerCursorInventory extends BaseInventory {

    public PlayerCursorInventory(CloudPlayer player) {
        super(player, InventoryType.UI);
    }

    /**
     * This override is here for documentation and code completion purposes only.
     *
     * @return Player
     */
    @Override
    public CloudPlayer getHolder() {
        return (CloudPlayer) super.getHolder();
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public void setSize(int size) {
        super.setSize(1);
    }
}
