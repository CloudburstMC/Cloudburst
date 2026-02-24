package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.inventory.view.BlockStorageView;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * Server-side implementation of {@link BlockStorageView} for chest-like containers
 * (chest, double chest, barrel, shulker box).
 *
 * <p>Wraps a {@link CloudContainer} (which may be the combined 54-slot container for
 * a double chest).</p>
 */
public class CloudChestView extends CloudSlotGroupBase implements BlockStorageView {

    private final Block block;

    public CloudChestView(SlotGroupType<BlockStorageView> slotGroupType, Block block, CloudContainer container) {
        super(slotGroupType, container);
        this.block = block;
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return block.getLevel().getBlockEntity(block.getPosition());
    }
}
