package org.cloudburstmc.api.item.data;

import java.util.Objects;

/**
 * A single page in a book.
 *
 * @param text      the text written on the page
 * @param photoName the photo name, or an empty string if there is no photo
 */
public record BookPage(String text, String photoName) {

    public BookPage {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(photoName, "photoName");
    }
}
