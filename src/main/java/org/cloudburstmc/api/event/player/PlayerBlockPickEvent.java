package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

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
