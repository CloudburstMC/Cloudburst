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
import org.cloudburstmc.nbt.NbtMap;

import javax.annotation.Nonnegative;

/**
 * Block entity implementation for a lectern. Stores a single book item and tracks the currently
 * displayed page number out of the book's total pages.
 */
public class LecternBlockEntity extends BaseBlockEntity implements Lectern {

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
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return LecternData.class;
    }

    @Override
    protected LecternData data() {
        return (LecternData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (!Boolean.TRUE.equals(data().getHasBook()) || data().getBook() == null) {
            // Without the book flag, or without the book itself, the book keys are
            // ignored, exactly as before: an empty lectern stores no book keys at all.
            data().setHasBook(null);
            data().setBook(null);
            data().setPage(null);
            data().setTotalPages(null);
        } else {
            // The book flag promises a complete book record; missing page keys
            // default exactly as before so the save writes them back.
            if (data().getPage() == null) {
                data().setPage(0);
            }
            if (data().getTotalPages() == null) {
                data().setTotalPages(0);
            }
        }
    }

    @Override
    public void onBreak() {
        if (data().getBook() != null) {
            this.dropItemOnBreak(data().getBook());
        }
    }

    @Override
    public boolean hasBook() {
        return data().getBook() != null;
    }

    @Override
    public ItemStack getBook() {
        ItemStack book = data().getBook();
        return book != null ? book : ItemStack.EMPTY;
    }

    @Override
    public void setBook(ItemStack item) {
        if (item != null && !item.isEmpty()
                && (item.getType() == ItemTypes.WRITABLE_BOOK || item.getType() == ItemTypes.WRITTEN_BOOK)) {
            data().setBook(item.withCount(1));
            data().setHasBook(true);
            if (data().getPage() == null) {
                data().setPage(0);
            }
        } else {
            data().setBook(null);
            data().setHasBook(null);
            data().setPage(null);
        }

        updateTotalPages(true);
    }

    @Override
    public int getTotalPages() {
        Integer totalPages = data().getTotalPages();
        return totalPages != null ? totalPages : 0;
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
        Integer page = data().getPage();
        return page != null ? page : 0;
    }

    @Override
    public void setPage(@Nonnegative int page) {
        data().setPage(GenericMath.clamp(page, 0, getTotalPages()));
        this.getLevel().updateAround(this.getPosition());
    }

    private void updateTotalPages(boolean updateRedstone) {
        if (hasBook()) {
            data().setTotalPages(data().getBook().get(ItemDataComponents.BOOK_DATA).getPages().size());
        } else {
            data().setTotalPages(null);
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
