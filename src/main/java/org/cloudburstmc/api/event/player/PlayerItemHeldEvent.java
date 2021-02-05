package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.Player;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PlayerItemHeldEvent extends PlayerEvent implements Cancellable {

    private final ItemStack item;
    private final int hotbarSlot;

    public PlayerItemHeldEvent(Player player, ItemStack item, int hotbarSlot) {
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

    public ItemStack getItem() {
        return item;
    }

}
