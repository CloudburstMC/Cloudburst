package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.GenericMath;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Lectern;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.item.data.WrittenBook;
import org.cloudburstmc.server.level.chunk.Chunk;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

public class LecternBlockEntity extends BaseBlockEntity implements Lectern {

    private static final String TAG_HAS_BOOK = "hasBook";
    private static final String TAG_BOOK = "book";
    private static final String TAG_PAGE = "page";
    private static final String TAG_TOTAL_PAGES = "totalPages";

    private ItemStack book;
    private int page;
    private int totalPages;

    public LecternBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockTypes.LECTERN;
    }

    @Override
    protected void init() {
        updateTotalPages(false);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        if (tag.getBoolean(TAG_HAS_BOOK)) {
            this.book = ItemUtils.deserializeItem(tag.getCompound(TAG_BOOK));
            this.page = tag.getInt(TAG_PAGE);
            this.totalPages = tag.getInt(TAG_TOTAL_PAGES);
        }
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        if (this.book != null) {
            tag.putBoolean(TAG_HAS_BOOK, true);
            tag.putCompound(TAG_BOOK, ItemUtils.serializeItem(this.book).toBuilder().build());
            tag.putInt(TAG_PAGE, this.page);
            tag.putInt(TAG_TOTAL_PAGES, this.totalPages);
        }
    }

    @Override
    public void onBreak() {
        if (this.book != null) {
            this.getLevel().dropItem(this.getPosition(), book);
        }
    }

    public boolean hasBook() {
        return this.book != null;
    }

    @Nullable
    public ItemStack getBook() {
        return book;
    }

    public void setBook(ItemStack item) {
        if (item != null && (item.getType() == ItemTypes.WRITTEN_BOOK || item.getType() == ItemTypes.WRITABLE_BOOK)) {
            this.book = item;
        } else {
            this.book = null;
        }

        updateTotalPages(true);
    }

    public int getLeftPage() {
        return (getPage() * 2) + 1;
    }

    public void setLeftPage(int newLeftPage) {
        setPage((newLeftPage - 1) / 2);
    }

    public int getRightPage() {
        return getLeftPage() + 1;
    }

    public void setRightPage(int newRightPage) {
        setLeftPage(newRightPage - 1);
    }

    @Override
    public int getPage() {
        return this.page;
    }

    @Override
    public void setPage(@Nonnegative int page) {
        this.page = GenericMath.clamp(page, 0, this.totalPages);
        this.getLevel().updateAround(this.getPosition());
    }

    @Override
    public int getTotalPages() {
        return totalPages;
    }

    private void updateTotalPages(boolean updateRedstone) {
        if (hasBook()) {
            this.totalPages = this.book.getMetadata(WrittenBook.class).getPages().size();
        } else {
            this.totalPages = 0;
        }

        if (updateRedstone) {
            this.getLevel().updateAroundRedstone(this.getPosition(), null);
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
