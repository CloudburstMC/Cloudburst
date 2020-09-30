package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.blockentity.ItemFrame;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.player.Player;

/**
 * Created by Pub4Game on 03.07.2016.
 */
public class ItemFrameDropItemEvent extends BlockEvent implements Cancellable {

    private final Player player;
    private final Item item;
    private final ItemFrame itemFrame;

    public ItemFrameDropItemEvent(Player player, Block block, ItemFrame itemFrame, Item item) {
        super(block);
        this.player = player;
        this.itemFrame = itemFrame;
        this.item = item;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemFrame getItemFrame() {
        return itemFrame;
    }

    public Item getItem() {
        return item;
    }
}