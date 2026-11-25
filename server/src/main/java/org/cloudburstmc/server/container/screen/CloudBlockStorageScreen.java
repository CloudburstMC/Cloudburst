package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.inventory.ScreenType;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.StorageScreen;
import org.cloudburstmc.api.inventory.view.BlockStorageView;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.blockentity.ContainerBlockEntity;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.view.CloudBlockStorageView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * A screen backed by a block's item storage.
 */
public class CloudBlockStorageScreen extends CloudBlockContainerScreen implements StorageScreen {

    protected final ContainerBlockEntity storageEntity;
    private final ContainerSlotType slotType;
    private final SlotGroupType<BlockStorageView> storageSlotGroupType;

    protected CloudBlockStorageScreen(
            ScreenType<? extends StorageScreen> type, CloudPlayer player, Block block,
            ContainerSlotType slotType, BlockEntityType<?> entityType,
            SlotGroupType<BlockStorageView> storageSlotGroupType) {
        this(type, player, block, slotType, getOrCreateBlockEntity(block, entityType, ContainerBlockEntity.class), storageSlotGroupType);
    }

    protected CloudBlockStorageScreen(ScreenType<? extends StorageScreen> type, CloudPlayer player, Block block,
            ContainerSlotType slotType, ContainerBlockEntity storageEntity,
            SlotGroupType<BlockStorageView> storageSlotGroupType) {
        super(type, player, block);
        this.storageEntity = storageEntity;
        this.slotType = slotType;
        this.storageSlotGroupType = storageSlotGroupType;
    }

    public static CloudBlockStorageScreen barrel(CloudPlayer player, Block block) {
        return new CloudBlockStorageScreen(ScreenTypes.BARREL, player, block, ContainerSlotType.BARREL, BlockEntityTypes.BARREL, SlotGroupTypes.BARREL);
    }

    @Override
    public BlockStorageView getStorage() {
        return this.getSlotsOrThrow(this.storageSlotGroupType);
    }

    @Override
    protected CloudContainer getStorageContainer() {
        return this.storageEntity.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        CloudBlockStorageView storageView = new CloudBlockStorageView(this.storageSlotGroupType, this.block, this.getStorageContainer());
        this.addMapping(new SimpleContainerMapping(this.slotType, storageView));
    }
}
