package org.cloudburstmc.api.inventory.view;

import org.checkerframework.checker.index.qual.NonNegative;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the item slot and page state of a lectern container.
 * The lectern holds a single book item and tracks the current open page.
 */
public interface LecternView extends SlotGroup {

    /**
     * Returns the book currently placed on the lectern.
     *
     * @return the book item, or {@link ItemStack#EMPTY} if no book is present
     */
    ItemStack getBook();

    /**
     * Sets the book on the lectern.
     *
     * @param item the book item to place; use {@link ItemStack#EMPTY} to remove the book
     */
    void setBook(ItemStack item);

    /**
     * Returns {@code true} if a book is currently placed on the lectern.
     *
     * @return {@code true} if a book is present
     */
    boolean hasBook();

    /**
     * Returns the total number of pages in the current book.
     * Returns {@code 0} if no book is present.
     *
     * @return total page count
     */
    @NonNegative
    int getTotalPages();

    /**
     * Returns the current open page index (0-based).
     *
     * @return the current page index
     */
    int getPage();

    /**
     * Sets the current open page index (0-based).
     * Values are clamped to the valid range {@code [0, getTotalPages()]}.
     *
     * @param page the page index to open
     */
    void setPage(@NonNegative int page);
}

