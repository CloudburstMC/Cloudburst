package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Lectern;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.LecternData;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3i;

import javax.annotation.Nonnegative;

/**
 * Block entity implementation for a lectern. Stores a single book item and tracks the currently
 * displayed page number out of the book's total pages.
 */
public class LecternBlockEntity extends BaseBlockEntity implements Lectern {

    private ItemStack book;
    private int page;
    private int totalPages;

    public LecternBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.LECTERN;
    }

    @Override
    public Lectern getBlockEntity() {
        return this;
    }

    @Override
    protected void init() {
        updateTotalPages(false);
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        LecternData lectern = (LecternData) data;
        if (Boolean.TRUE.equals(lectern.getHasBook())) {
            this.book = lectern.getBook();
            this.page = lectern.getPage() != null ? lectern.getPage() : 0;
            this.totalPages = lectern.getTotalPages() != null ? lectern.getTotalPages() : 0;
        }
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        LecternData lectern = (LecternData) data;
        if (this.book != null) {
            lectern.setHasBook(true);
            lectern.setBook(this.book);
            lectern.setPage(this.page);
            lectern.setTotalPages(this.totalPages);
        }
    }

    @Override
    public void onBreak() {
        if (this.book != null) {
            this.dropItemOnBreak(this.book);
        }
    }

    @Override
    public boolean hasBook() {
        return this.book != null;
    }

    @Override
    public ItemStack getBook() {
        return this.book != null ? this.book : ItemStack.EMPTY;
    }

    @Override
    public void setBook(ItemStack item) {
        if (item != null && !item.isEmpty()
                && (item.getType() == ItemTypes.WRITABLE_BOOK || item.getType() == ItemTypes.WRITTEN_BOOK)) {
            this.book = item.withCount(1);
        } else {
            this.book = null;
        }

        updateTotalPages(true);
    }

    @Override
    public int getTotalPages() {
        return totalPages;
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

    private void updateTotalPages(boolean updateRedstone) {
        if (hasBook()) {
            this.totalPages = this.book.get(ItemDataComponents.BOOK_DATA).getPages().size();
        } else {
            this.totalPages = 0;
        }

        if (updateRedstone) {
            this.getLevel().updateAroundRedstone(this.getPosition(), null);
        }
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot != 0) throw new IndexOutOfBoundsException("Lectern has only 1 slot, index: " + slot);
        return getBook();
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        if (slot != 0) throw new IndexOutOfBoundsException("Lectern has only 1 slot, index: " + slot);
        setBook(itemStack);
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
