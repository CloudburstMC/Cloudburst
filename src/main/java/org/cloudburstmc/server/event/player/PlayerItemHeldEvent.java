package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.player.Player;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PlayerItemHeldEvent extends PlayerEvent implements Cancellable {

    private final Item item;
    private final int hotbarSlot;

    public PlayerItemHeldEvent(Player player, Item item, int hotbarSlot) {
        super(player);
        this.item = item;
        this.hotbarSlot = hotbarSlot;
    }

    public int getSlot() {
        return this.hotbarSlot;
    }

    @Deprecated
    public int getInventorySlot() {
        return hotbarSlot;
    }

    public Item getItem() {
        return item;
    }

}
