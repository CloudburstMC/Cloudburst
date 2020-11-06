package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;

/**
 * @author CreeperFace
 */
public class PlayerBlockPickEvent extends PlayerEvent implements Cancellable {

    private final Block blockStateClicked;
    private ItemStack item;

    public PlayerBlockPickEvent(Player player, Block blockClicked, ItemStack item) {
        super(player);
        this.blockStateClicked = blockClicked;
        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public Block getBlockClicked() {
        return blockStateClicked;
    }
}
