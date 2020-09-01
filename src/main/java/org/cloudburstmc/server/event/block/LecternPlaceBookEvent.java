package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.blockentity.Lectern;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.player.Player;

public class LecternPlaceBookEvent extends BlockEvent implements Cancellable {

    private final Player player;
    private final Lectern lectern;
    private Item book;

    public LecternPlaceBookEvent(Player player, Lectern lectern, Item book) {
        super(lectern.getBlock());
        this.player = player;
        this.lectern = lectern;
        this.book = book;
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
