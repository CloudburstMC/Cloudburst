package org.cloudburstmc.server.event.block;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.blockentity.Lectern;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.player.Player;

public class LecternPlaceBookEvent extends BlockEvent implements Cancellable {

    private final Player player;
    private final Lectern lectern;
    private ItemStack book;

    public LecternPlaceBookEvent(Player player, Lectern lectern, ItemStack book) {
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

    public ItemStack getBook() {
        return book;
    }

    public void setBook(ItemStack book) {
        this.book = book;
    }

}
