package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.inventory.view.BlockStorageView;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.server.container.CloudContainer;

public class CloudBlockStorageView extends CloudSlotGroupBase implements BlockStorageView {

    private final Block block;

    public CloudBlockStorageView(SlotGroupType<BlockStorageView> slotGroupType, Block block, CloudContainer container) {
        super(slotGroupType, container);
        this.block = block;
    }

    @Override
    public Block getBlock() {
        return this.block;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this.block.getLevel().getBlockEntity(this.block.getPosition());
    }
}
