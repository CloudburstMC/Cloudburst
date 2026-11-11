package org.cloudburstmc.api.item.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * The pages and signature data of a writable or written book.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class WrittenBook {

    /**
     * The copy generation. An original book has generation zero.
     */
    private final int generation;

    /**
     * The displayed title. A writable book has no title.
     */
    private final String title;

    /**
     * The displayed author name, which is not proof of the author's identity.
     */
    private final String author;

    /**
     * The Xbox user ID recorded in the signature, when available.
     */
    private final String xuid;

    /**
     * The identifier recorded in the book data, if present.
     */
    private final long id;

    /**
     * Whether the book data marks its page content as resolved.
     */
    private final boolean resolved;

    /**
     * The unmodifiable list of pages in reading order.
     */
    private final List<BookPage> pages;

    /**
     * Creates the page and signature data for a book.
     *
     * @param generation the copy generation, with zero for an original
     * @param title      the displayed title, or an empty string for a draft
     * @param author     the displayed author name, or an empty string for a draft
     * @param xuid       the signer's Xbox user ID, or an empty string if unavailable
     * @param id         the recorded book identifier, or zero if unavailable
     * @param resolved   whether the page content is marked as resolved
     * @param pages      the pages in reading order, copied when the book is created
     * @return the book data
     */
    public static WrittenBook of(int generation, String title, String author, String xuid, long id, boolean resolved, List<BookPage> pages) {
        return new WrittenBook(generation, Objects.requireNonNull(title, "title"), author, xuid, id, resolved, List.copyOf(pages));
    }
}
