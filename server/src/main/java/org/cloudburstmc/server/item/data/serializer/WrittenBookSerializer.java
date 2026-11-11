package org.cloudburstmc.server.item.data.serializer;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.Page;
import org.cloudburstmc.api.item.data.WrittenBook;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.utils.PageContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializes written book data, including title, author, generation, resolved state, and pages.
 */
public class WrittenBookSerializer implements ItemDataComponentSerializer<WrittenBook> {

    private static final String TAG_TITLE = "title";
    private static final String TAG_AUTHOR = "author";
    private static final String TAG_XUID = "xuid";
    private static final String TAG_PAGES = "pages";
    private static final String TAG_GENERATION = "generation";
    private static final String TAG_RESOLVED = "resolved";
    private static final String TAG_ID = "id";

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, WrittenBook value) {
        if (item.getType() == ItemTypes.WRITTEN_BOOK) {
            tag.putInt(TAG_GENERATION, value.getGeneration());
            tag.putString(TAG_TITLE, value.getTitle());
            tag.putString(TAG_AUTHOR, value.getAuthor());
            tag.putString(TAG_XUID, value.getXuid());
        }

        List<NbtMap> pages = new ArrayList<>();
        for (Page page : value.getPages()) {
            pages.add(PageContent.from(page.getText(), page.getPhotoName()).createTag());
        }

        tag.putList(TAG_PAGES, NbtType.COMPOUND, pages);
    }

    @Override
    public WrittenBook deserialize(Identifier id, NbtMap tag) {
        if (!tag.containsKey(TAG_TITLE) && !tag.containsKey(TAG_AUTHOR) && !tag.containsKey(TAG_PAGES)) {
            return null;
        }

        List<Page> pages;
        List<NbtMap> pageTags = tag.getList(TAG_PAGES, NbtType.COMPOUND);

        if (pageTags == null) {
            pages = ImmutableList.of();
        } else {
            pages = new ArrayList<>(pageTags.size());

            for (NbtMap pageTag : pageTags) {
                pages.add(PageContent.from(pageTag));
            }
        }

        return WrittenBook.of(
                tag.getInt(TAG_GENERATION, 0),
                tag.getString(TAG_TITLE, ""),
                tag.getString(TAG_AUTHOR, ""),
                tag.getString(TAG_XUID, ""),
                tag.getLong(TAG_ID, 0L),
                tag.getBoolean(TAG_RESOLVED, false),
                pages
        );
    }
}
