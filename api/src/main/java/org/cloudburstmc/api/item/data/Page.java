package org.cloudburstmc.api.item.data;

/**
 * The text and optional photo on a single book page.
 */
public interface Page {

    /**
     * Returns whether the page meets the book editor's content limits.
     */
    boolean isValid();

    /**
     * Returns the text written on the page.
     */
    String getText();

    /**
     * Returns the photo name, or an empty string if the page has no photo.
     */
    String getPhotoName();
}
