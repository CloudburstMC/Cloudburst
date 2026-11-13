package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Lectern;
import org.cloudburstmc.api.event.inventory.InventoryCloseEvent;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.WrittenBook;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.component.LecternBlockHandlers;
import org.cloudburstmc.server.blockentity.data.CloudBlockEntityData;
import org.cloudburstmc.server.blockentity.data.CloudLecternData;
import org.cloudburstmc.server.container.screen.CloudLecternContainerScreen;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Objects;

public class LecternBlockEntity extends BaseBlockEntity implements Lectern {

    public LecternBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    public static boolean isBook(ItemStack item) {
        return !item.isEmpty() && (item.getType() == ItemTypes.WRITABLE_BOOK || item.getType() == ItemTypes.WRITTEN_BOOK);
    }

    @Override
    protected Class<? extends CloudBlockEntityData> minimumDataClass() {
        return CloudLecternData.class;
    }

    @Override
    protected CloudLecternData data() {
        return (CloudLecternData) super.data();
    }

    @Override
    public Lectern getBlockEntity() {
        return this;
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.LECTERN;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ItemStack getItem(int slot) {
        Objects.checkIndex(slot, this.size());
        return this.getBook();
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        Objects.checkIndex(slot, this.size());
        this.setBook(item);
    }

    @Override
    public boolean hasBook() {
        return !this.getBook().isEmpty();
    }

    @Override
    public ItemStack getBook() {
        return Objects.requireNonNullElse(this.data().getBook(), ItemStack.EMPTY);
    }

    @Override
    public void setBook(ItemStack item) {
        if (!item.isEmpty() && !isBook(item)) {
            throw new IllegalArgumentException("Lecterns only accept writable or written books");
        }

        ItemStack next = item.isEmpty() ? ItemStack.EMPTY : item.withCount(1);
        if (this.getBook().equals(next)) {
            return;
        }

        this.storeBook(next);
        this.data().setPage(next.isEmpty() ? null : 0);
        this.publishChange();
        if (next.isEmpty()) {
            this.closeReaders();
        }
    }

    @Override
    public int getTotalPages() {
        WrittenBook data = this.getBook().get(ItemDataComponents.BOOK_DATA);
        return data == null ? 0 : data.getPages().size();
    }

    @Override
    public int getPage() {
        return Objects.requireNonNullElse(this.data().getPage(), 0);
    }

    @Override
    public void setPage(int page) {
        int next = Math.clamp(page, 0, this.lastPage());
        if (next != this.getPage()) {
            this.data().setPage(next);
            this.publishChange();
            LecternBlockHandlers.pulse(this.getBlock());
        }
    }

    public int lastPage() {
        return Math.max(0, (this.getTotalPages() - 1) / 2);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        ItemStack stored = Boolean.TRUE.equals(this.data().getHasBook()) ? this.getBook() : ItemStack.EMPTY;
        this.storeBook(isBook(stored) ? stored.withCount(1) : ItemStack.EMPTY);
        this.data().setPage(this.hasBook() ? Math.clamp(tag.getInt("page", 0), 0, this.lastPage()) : null);
    }

    @Override
    public void saveCustomClient(NbtMapBuilder tag) {
        super.saveCustomClient(tag);
        Vector3i position = this.getPosition();
        tag.putInt("x", position.getX()).putInt("y", position.getY()).putInt("z", position.getZ());
        tag.putInt("BlockEntityVersion", 0);
        this.writeBookData(tag);
    }

    private void writeBookData(NbtMapBuilder tag) {
        tag.putBoolean("hasBook", this.hasBook());
        if (this.hasBook()) {
            tag.putCompound("book", ItemStackNbtCodec.serializeItem(this.getBook()).toBuilder()
                    .putBoolean("WasPickedUp", false).build());
            tag.putInt("page", this.getPage());
            tag.putInt("totalPages", this.getTotalPages());
        }
    }

    private void storeBook(ItemStack book) {
        this.data().setBook(book.isEmpty() ? null : book);
        this.data().setHasBook(book.isEmpty() ? null : true);
        this.data().setTotalPages(this.hasBook() ? this.getTotalPages() : null);
    }

    private void publishChange() {
        this.setDirty();
        this.spawnToAll();
        this.getLevel().updateAroundRedstone(this.getPosition(), null);
        this.getLevel().updateComparatorOutputLevel(this.getPosition());
    }

    @Override
    public void onBreak() {
        ItemStack stored = this.getBook();
        this.storeBook(ItemStack.EMPTY);
        this.data().setPage(null);
        this.closeReaders();
        if (!stored.isEmpty()) {
            this.getLevel().dropBlockContents(this.getPosition(), stored);
        }
    }

    private void closeReaders() {
        for (CloudPlayer viewer : this.getChunk().getViewers()) {
            if (viewer.getInventoryManager().getScreen() instanceof CloudLecternContainerScreen screen && screen.lecternEntity() == this) {
                viewer.getInventoryManager().closeScreen(InventoryCloseEvent.Reason.CANT_USE);
            }
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
