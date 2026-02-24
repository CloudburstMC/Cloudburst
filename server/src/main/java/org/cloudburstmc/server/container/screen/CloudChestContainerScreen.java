package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.ScreenType;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.StorageScreen;
import org.cloudburstmc.api.inventory.view.BlockStorageView;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.blockentity.ChestBlockEntity;
import org.cloudburstmc.server.blockentity.ContainerBlockEntity;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.view.CloudChestView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for chest-like block containers: chest, barrel, and shulker box.
 * For ender chest see {@link CloudEnderChestScreen}.
 *
 * <p>When a chest is paired with an adjacent chest to form a double chest,
 * {@link #chest(CloudPlayer, Block)} automatically detects the pairing and
 * returns a screen with type {@link ScreenTypes#DOUBLE_CHEST} and slot group
 * type {@link SlotGroupTypes#DOUBLE_CHEST} (54 slots).</p>
 */
public class CloudChestContainerScreen extends CloudBlockContainerScreen implements StorageScreen {

    private final ContainerBlockEntity chestEntity;
    private final ContainerSlotType slotType;
    private final SlotGroupType<BlockStorageView> storageSlotGroupType;

    private CloudChestContainerScreen(ScreenType<StorageScreen> type, CloudPlayer player, Block block,
                                      ContainerSlotType slotType, BlockEntityType<?> beType,
                                      SlotGroupType<BlockStorageView> storageSlotGroupType) {
        super(type, player, block);
        this.chestEntity = getOrCreateBlockEntity(block, beType);
        this.slotType = slotType;
        this.storageSlotGroupType = storageSlotGroupType;
    }

    private CloudChestContainerScreen(ScreenType<StorageScreen> type, CloudPlayer player, Block block,
                                      ContainerSlotType slotType, ContainerBlockEntity be,
                                      SlotGroupType<BlockStorageView> storageSlotGroupType) {
        super(type, player, block);
        this.chestEntity = be;
        this.slotType = slotType;
        this.storageSlotGroupType = storageSlotGroupType;
    }

    public static CloudChestContainerScreen chest(CloudPlayer player, Block block) {
        ChestBlockEntity be = getOrCreateBlockEntity(block, BlockEntityTypes.CHEST);
        boolean paired = be.isPaired();
        return new CloudChestContainerScreen(
                paired ? ScreenTypes.DOUBLE_CHEST : ScreenTypes.CHEST,
                player, block,
                ContainerSlotType.LEVEL_ENTITY, be,
                paired ? SlotGroupTypes.DOUBLE_CHEST : SlotGroupTypes.CHEST);
    }

    public static CloudChestContainerScreen barrel(CloudPlayer player, Block block) {
        return new CloudChestContainerScreen(ScreenTypes.BARREL, player, block,
                ContainerSlotType.BARREL, BlockEntityTypes.BARREL,
                SlotGroupTypes.BARREL);
    }

    public static CloudChestContainerScreen shulkerBox(CloudPlayer player, Block block) {
        return new CloudChestContainerScreen(ScreenTypes.SHULKER_BOX, player, block,
                ContainerSlotType.SHULKER_BOX, BlockEntityTypes.SHULKER_BOX,
                SlotGroupTypes.SHULKER_BOX);
    }

    @Override
    protected Container getStorageContainer() {
        return chestEntity.getContainer();
    }

    @Override
    public BlockStorageView getStorage() {
        return getSlotsOrThrow(storageSlotGroupType);
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        CloudChestView storageView = new CloudChestView(storageSlotGroupType, block, chestEntity.getContainer());
        this.addMapping(new SimpleContainerMapping(slotType, storageView));
    }
}
