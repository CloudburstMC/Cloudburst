package org.cloudburstmc.api.item.data;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.concurrent.Immutable;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Immutable
public class WrittenBook {

    private final int generation; //TODO: nullable properties?
    private final String title;
    private final String author;
    private final String xuid;
    private final long id;
    private final boolean resolved;
    private final List<Page> pages;

    public static WrittenBook of(int generation, String title, String author, String xuid, long id, boolean resolved, List<Page> pages) {
        Preconditions.checkNotNull(title, "title");
        Preconditions.checkNotNull(pages, "pages");
        return new WrittenBook(generation, title, author, xuid, id, resolved, pages);
    }
}
