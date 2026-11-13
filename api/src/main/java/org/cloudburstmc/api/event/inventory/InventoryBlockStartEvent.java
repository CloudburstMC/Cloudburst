package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.block.BlockEvent;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Objects;

/**
 * Base event for a block accepting an ingredient for processing.
 */
public class InventoryBlockStartEvent extends BlockEvent {

    private final ItemStack source;

    public InventoryBlockStartEvent(Block block, ItemStack source) {
        super(block);
        this.source = Objects.requireNonNull(source, "source");
    }

    /**
     * @return the ingredient being processed
     */
    public ItemStack getSource() {
        return this.source;
    }
}
