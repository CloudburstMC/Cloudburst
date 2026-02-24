package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.Lectern;
import org.cloudburstmc.api.inventory.view.BlockLecternView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * Server-side implementation of {@link BlockLecternView}.
 * Wraps a 1-slot container: slot 0 = book.
 *
 * <p>For page and total-pages state, this delegates to the backing
 * {@link Lectern} block entity.</p>
 */
public class CloudLecternView extends CloudSlotGroupBase implements BlockLecternView {

    private final Block block;

    public CloudLecternView(Block block, CloudContainer container) {
        super(SlotGroupTypes.LECTERN, container);
        this.block = block;
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public Lectern getBlockEntity() {
        return (Lectern) block.getLevel().getBlockEntity(block.getPosition());
    }

    @Override
    public ItemStack getBook() {
        return getItem(0);
    }

    @Override
    public void setBook(ItemStack item) {
        setItem(0, item);
    }

    @Override
    public boolean hasBook() {
        Lectern entity = getBlockEntity();
        return entity != null && entity.hasBook();
    }

    @Override
    public int getTotalPages() {
        Lectern entity = getBlockEntity();
        return entity != null ? entity.getTotalPages() : 0;
    }

    @Override
    public int getPage() {
        Lectern entity = getBlockEntity();
        return entity != null ? entity.getPage() : 0;
    }

    @Override
    public void setPage(int page) {
        Lectern entity = getBlockEntity();
        if (entity != null) {
            entity.setPage(page);
        }
    }
}

