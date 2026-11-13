package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.Lectern;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Called before a player inserts a book into an empty lectern.
 * Cancelling prevents the insertion and leaves the held book unchanged.
 */
public class PlayerInsertLecternBookEvent extends PlayerEvent implements Cancellable {

    private final Lectern lectern;
    private ItemStack book;

    /**
     * Creates a book insertion event.
     *
     * @param player  the player inserting the book
     * @param lectern the lectern receiving the book
     * @param book    the writable or written book proposed for insertion
     */
    public PlayerInsertLecternBookEvent(Player player, Lectern lectern, ItemStack book) {
        super(player);
        this.lectern = lectern;
        this.book = book;
    }

    /**
     * Returns the lectern receiving the book.
     */
    public Lectern getLectern() {
        return this.lectern;
    }

    /**
     * Returns the block of the lectern receiving the book.
     */
    public Block getBlock() {
        return this.lectern.getBlock();
    }

    /**
     * Returns the book proposed for insertion.
     */
    public ItemStack getBook() {
        return this.book;
    }

    /**
     * Sets the writable or written book to insert. Only one copy is placed.
     *
     * @param book the proposed replacement for the book being inserted
     */
    public void setBook(ItemStack book) {
        this.book = Objects.requireNonNull(book, "book");
    }
}
