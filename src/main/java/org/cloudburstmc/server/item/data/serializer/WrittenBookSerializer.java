package org.cloudburstmc.server.item.data.serializer;

import com.google.common.collect.ImmutableList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.data.WrittenBook;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.PageContent;

import java.util.ArrayList;
import java.util.List;

public class WrittenBookSerializer implements ItemDataSerializer<WrittenBook> {

    private static final int MAX_GENERATION = 2;
    private static final int MAX_PAGES = 50;
    private static final int MAX_TITLE_LENGTH = 16;

    private static final String TAG_TITLE = "title";
    private static final String TAG_AUTHOR = "author";
    private static final String TAG_XUID = "xuid";
    private static final String TAG_PAGES = "pages";
    private static final String TAG_GENERATION = "generation";
    private static final String TAG_RESOLVED = "resolved";
    private static final String TAG_ID = "id";

    @Override
    public void serialize(ItemStack item, NbtMapBuilder rootTag, NbtMapBuilder dataTag, WrittenBook value) {
        dataTag.putInt(TAG_GENERATION, value.getGeneration());
        dataTag.putString(TAG_TITLE, value.getTitle());
        dataTag.putString(TAG_AUTHOR, value.getAuthor());
        dataTag.putString(TAG_XUID, value.getXuid());
        dataTag.putLong(TAG_ID, value.getId());
        dataTag.putBoolean(TAG_RESOLVED, value.isResolved());

        List<NbtMap> pages = new ArrayList<>();
        for (PageContent page : value.getPages()) {
            pages.add(page.createTag());
        }
        dataTag.putList(TAG_PAGES, NbtType.COMPOUND, pages);
    }

    @Override
    public WrittenBook deserialize(Identifier id, NbtMap rootTag, NbtMap dataTag) {
        List<PageContent> pages;
        List<NbtMap> pageTags = dataTag.getList(TAG_PAGES, NbtType.COMPOUND);

        if (pageTags == null) {
            pages = ImmutableList.of();
        } else {
            pages = new ArrayList<>(pageTags.size());

            for (NbtMap pageTag : pageTags) {
                pages.add(PageContent.from(pageTag));
            }
        }

        return WrittenBook.of(
                dataTag.getInt(TAG_GENERATION),
                dataTag.getString(TAG_TITLE),
                dataTag.getString(TAG_AUTHOR),
                dataTag.getString(TAG_XUID),
                dataTag.getLong(TAG_ID),
                dataTag.getBoolean(TAG_RESOLVED),
                pages
        );
    }
}
