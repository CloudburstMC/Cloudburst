package org.cloudburstmc.api.inventory.view;

import org.checkerframework.checker.index.qual.NonNegative;
import org.cloudburstmc.api.item.ItemStack;

/**
 * The book slot and shared reading position of a lectern.
 */
public interface LecternView extends SlotGroup {

    /**
     * Returns the book currently placed on the lectern.
     *
     * @return the book item, or {@link ItemStack#EMPTY} if no book is present
     */
    ItemStack getBook();

    /**
     * Sets the book on the lectern. Only one copy is stored.
     * Changing the book resets the reading position. Removing it closes the readers' screens.
     *
     * @param item a writable book, a written book, or {@link ItemStack#EMPTY} to remove the book
     * @throws IllegalArgumentException if the item is not a book and is not empty
     */
    void setBook(ItemStack item);

    /**
     * Returns whether the lectern holds a book, including a blank writable book.
     */
    boolean hasBook();

    /**
     * Returns the number of individual pages, or {@code 0} if the lectern has no book or the book has no pages.
     */
    @NonNegative
    int getTotalPages();

    /**
     * Returns the zero-based index of the current two-page spread.
     *
     * @return the current spread index, or {@code 0} if there is no book
     */
    int getPage();

    /**
     * Sets the zero-based spread index, clamped to the book's available spreads.
     * Changing the spread updates all readers and triggers a redstone pulse.
     *
     * @param page the spread index to open
     */
    void setPage(@NonNegative int page);
}

