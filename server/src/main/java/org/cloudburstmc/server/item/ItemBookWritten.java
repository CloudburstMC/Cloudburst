package org.cloudburstmc.server.item;

import com.google.common.collect.ImmutableList;
import com.nukkitx.math.GenericMath;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.PageContent;

import java.util.ArrayList;
import java.util.List;

public class ItemBookWritten extends Item {

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

    protected boolean isWritten = false;
    protected final List<PageContent> pages = new ArrayList<>();
    protected int generation;
    protected String title;
    protected String author;
    protected String xuid;
    protected long id;
    protected boolean resolved;

    public ItemBookWritten(Identifier id) {
        super(id);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForInt(TAG_GENERATION, this::setGeneration);
        tag.listenForString(TAG_TITLE, this::setTitle);
        tag.listenForString(TAG_AUTHOR, this::setAuthor);
        tag.listenForString(TAG_XUID, this::setXuid);
        tag.listenForLong(TAG_ID, this::setBookId);
        tag.listenForBoolean(TAG_RESOLVED, this::setResolved);
        tag.listenForList(TAG_PAGES, NbtType.COMPOUND, tags -> {
            this.pages.clear();
            for (NbtMap pageTag : tags) {
                this.pages.add(PageContent.from(pageTag));
            }
        });
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putInt(TAG_GENERATION, this.getGeneration());
        tag.putString(TAG_TITLE, this.getTitle());
        tag.putString(TAG_AUTHOR, this.getAuthor());
        tag.putString(TAG_XUID, this.getXuid());
        tag.putLong(TAG_ID, this.getBookId());
        tag.putBoolean(TAG_RESOLVED, this.isResolved());

        List<NbtMap> pages = new ArrayList<>();
        for (PageContent page : this.pages) {
            pages.add(page.createTag());
        }
        tag.putList(TAG_PAGES, NbtType.COMPOUND, pages);
    }

    public void signBook(String title, String author, String xuid) {
        this.setAuthor(author);
        this.setXuid(xuid);
        this.setTitle(title);
        this.setGeneration(0);
    }

    public int getGeneration() {
        return generation;
    }

    public void setGeneration(int generation) {
        this.generation = GenericMath.clamp(generation, 0, MAX_GENERATION);
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getXuid() {
        return xuid;
    }

    public void setXuid(String xuid) {
        this.xuid = xuid;
    }

    public long getBookId() {
        return this.id;
    }

    public void setBookId(long id) {
        this.id = id;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public List<PageContent> getPages() {
        return ImmutableList.copyOf(pages);
    }
}