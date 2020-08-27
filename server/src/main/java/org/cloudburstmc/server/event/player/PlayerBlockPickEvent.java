package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.player.Player;

/**
 * @author CreeperFace
 */
public class PlayerBlockPickEvent extends PlayerEvent implements Cancellable {

    private final Block blockStateClicked;
    private Item item;

    public PlayerBlockPickEvent(Player player, Block blockClicked, Item item) {
        super(player);
        this.blockStateClicked = blockClicked;
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Block getBlockClicked() {
        return blockStateClicked;
    }
}
