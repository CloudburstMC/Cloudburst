package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Called before a player edits or signs a writable book. Cancelling leaves the original book unchanged.
 */
public class PlayerEditBookEvent extends PlayerEvent implements Cancellable {

    private final int slot;
    private final ItemStack previousBook;
    private final boolean signing;
    private ItemStack newBook;

    public PlayerEditBookEvent(Player player, int slot, ItemStack previousBook, ItemStack newBook, boolean signing) {
        super(player);
        this.slot = slot;
        this.previousBook = previousBook;
        this.newBook = newBook;
        this.signing = signing;
    }

    /**
     * Returns the zero-based hotbar slot of the edited book.
     */
    public int getSlot() {
        return this.slot;
    }

    /**
     * Returns the writable book before the edit.
     */
    public ItemStack getPreviousBook() {
        return this.previousBook;
    }

    /**
     * Returns the proposed replacement book.
     */
    public ItemStack getNewBook() {
        return this.newBook;
    }

    /**
     * Sets the replacement book. Use a written book when signing and a writable book otherwise.
     * The original stack count is retained.
     */
    public void setNewBook(ItemStack book) {
        this.newBook = Objects.requireNonNull(book, "book");
    }

    /**
     * Returns whether this edit signs the book.
     */
    public boolean isSigning() {
        return this.signing;
    }
}
