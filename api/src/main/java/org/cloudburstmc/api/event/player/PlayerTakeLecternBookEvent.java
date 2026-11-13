package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.Lectern;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

/**
 * Called before a player removes a book from a lectern.
 * Cancelling leaves the book on the lectern.
 */
public class PlayerTakeLecternBookEvent extends PlayerEvent implements Cancellable {

    private final Lectern lectern;
    private final ItemStack book;

    /**
     * Creates a book removal event.
     *
     * @param player  the player removing the book
     * @param lectern the lectern holding the book
     * @param book    the book being removed
     */
    public PlayerTakeLecternBookEvent(Player player, Lectern lectern, ItemStack book) {
        super(player);
        this.lectern = lectern;
        this.book = book;
    }

    /**
     * Returns the lectern holding the book.
     */
    public Lectern getLectern() {
        return this.lectern;
    }

    /**
     * Returns the block of the lectern holding the book.
     */
    public Block getBlock() {
        return this.lectern.getBlock();
    }

    /**
     * Returns the book the player is attempting to remove.
     */
    public ItemStack getBook() {
        return this.book;
    }
}
