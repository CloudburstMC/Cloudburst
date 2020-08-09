package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.blockentity.Lectern;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.player.Player;

public class LecternDropBookEvent extends BlockEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Lectern lectern;
    private Item book;

    public LecternDropBookEvent(Player player, Lectern lectern, Item book) {
        super(lectern.getBlock());

        this.player = player;
        this.lectern = lectern;
        this.book = book;
    }

    public static HandlerList getHandlers() {
        return handlers;
    }

    public Lectern getLectern() {
        return lectern;
    }

    public Player getPlayer() {
        return player;
    }

    public Item getBook() {
        return book.clone();
    }

    public void setBook(Item book) {
        this.book = book;
    }

}
