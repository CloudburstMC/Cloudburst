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
import org.cloudburstmc.server.container.screen.CloudLecternContainerScreen;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Objects;

public class LecternBlockEntity extends BaseBlockEntity implements Lectern {

    private ItemStack book = ItemStack.EMPTY;
    private int page;

    public LecternBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    public static boolean isBook(ItemStack item) {
        return !item.isEmpty() && (item.getType() == ItemTypes.WRITABLE_BOOK || item.getType() == ItemTypes.WRITTEN_BOOK);
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
        return !this.book.isEmpty();
    }

    @Override
    public ItemStack getBook() {
        return this.book;
    }

    @Override
    public void setBook(ItemStack item) {
        if (!item.isEmpty() && !isBook(item)) {
            throw new IllegalArgumentException("Lecterns only accept writable or written books");
        }

        ItemStack next = item.isEmpty() ? ItemStack.EMPTY : item.withCount(1);
        if (this.book.equals(next)) {
            return;
        }

        this.book = next;
        this.page = 0;
        this.publishChange();
        if (next.isEmpty()) {
            this.closeReaders();
        }
    }

    @Override
    public int getTotalPages() {
        WrittenBook data = this.book.get(ItemDataComponents.BOOK_DATA);
        return data == null ? 0 : data.getPages().size();
    }

    @Override
    public int getPage() {
        return this.page;
    }

    @Override
    public void setPage(int page) {
        int next = Math.clamp(page, 0, this.lastPage());
        if (next != this.page) {
            this.page = next;
            this.publishChange();
            LecternBlockHandlers.pulse(this.getBlock());
        }
    }

    public int lastPage() {
        return Math.max(0, (this.getTotalPages() - 1) / 2);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        ItemStack stored = ItemStackNbtCodec.deserializeItem(tag.getCompound("book", NbtMap.EMPTY));
        this.book = isBook(stored) ? stored.withCount(1) : ItemStack.EMPTY;
        this.page = Math.clamp(tag.getInt("page", 0), 0, this.lastPage());
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        this.writeBookData(tag);
    }

    @Override
    public void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);
        Vector3i position = this.getPosition();
        tag.putInt("x", position.getX()).putInt("y", position.getY()).putInt("z", position.getZ());
        tag.putInt("BlockEntityVersion", 0);
        this.writeBookData(tag);
    }

    private void writeBookData(NbtMapBuilder tag) {
        tag.putBoolean("hasBook", this.hasBook());
        if (this.hasBook()) {
            tag.putCompound("book", ItemStackNbtCodec.serializeItem(this.book).toBuilder()
                    .putBoolean("WasPickedUp", false).build());
            tag.putInt("page", this.page);
            tag.putInt("totalPages", this.getTotalPages());
        }
    }

    private void publishChange() {
        this.setDirty();
        this.spawnToAll();
        this.getLevel().updateAroundRedstone(this.getPosition(), null);
        this.getLevel().updateComparatorOutputLevel(this.getPosition());
    }

    @Override
    public void onBreak() {
        ItemStack stored = this.book;
        this.book = ItemStack.EMPTY;
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
