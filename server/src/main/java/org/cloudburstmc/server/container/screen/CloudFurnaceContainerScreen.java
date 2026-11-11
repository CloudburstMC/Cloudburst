package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.inventory.FurnaceScreen;
import org.cloudburstmc.api.inventory.ScreenType;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.BlockFurnaceView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.blockentity.FurnaceBlockEntity;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudFurnaceView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for furnace-like block containers: furnace, blast furnace, and smoker.
 */
public class CloudFurnaceContainerScreen extends CloudBlockContainerScreen implements FurnaceScreen {

    private final FurnaceBlockEntity furnaceEntity;
    private final ContainerSlotType ingredientSlotType;
    private final CloudFurnaceView furnaceView;

    private CloudFurnaceContainerScreen(ScreenType<FurnaceScreen> type, CloudPlayer player, Block block, ContainerSlotType ingredientSlotType, BlockEntityType<?> blockEntityType) {
        super(type, player, block);
        this.furnaceEntity = getOrCreateBlockEntity(block, blockEntityType);
        this.furnaceView = new CloudFurnaceView(this.furnaceEntity);
        this.ingredientSlotType = ingredientSlotType;
    }

    public static CloudFurnaceContainerScreen furnace(CloudPlayer player, Block block) {
        return new CloudFurnaceContainerScreen(ScreenTypes.FURNACE, player, block, ContainerSlotType.FURNACE_INGREDIENT, BlockEntityTypes.FURNACE);
    }

    public static CloudFurnaceContainerScreen blastFurnace(CloudPlayer player, Block block) {
        return new CloudFurnaceContainerScreen(ScreenTypes.BLAST_FURNACE, player, block, ContainerSlotType.BLAST_FURNACE_INGREDIENT, BlockEntityTypes.BLAST_FURNACE);
    }

    public static CloudFurnaceContainerScreen smoker(CloudPlayer player, Block block) {
        return new CloudFurnaceContainerScreen(ScreenTypes.SMOKER, player, block, ContainerSlotType.SMOKER_INGREDIENT, BlockEntityTypes.SMOKER);
    }

    @Override
    protected Container getStorageContainer() {
        return this.furnaceEntity.getContainer();
    }

    @Override
    public void open() {
        if (this.furnaceEntity.hasCustomName()) {
            this.furnaceEntity.spawnTo(this.player);
        }
        super.open();
        this.furnaceEntity.sendProgress(this.player);
    }

    public FurnaceBlockEntity furnaceEntity() {
        return this.furnaceEntity;
    }

    @Override
    public BlockFurnaceView getFurnace() {
        return this.furnaceView;
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.mapSlot(this.ingredientSlotType, FurnaceBlockEntity.INGREDIENT_SLOT);
        this.mapSlot(ContainerSlotType.FURNACE_FUEL, FurnaceBlockEntity.FUEL_SLOT);
        this.mapSlot(ContainerSlotType.FURNACE_RESULT, FurnaceBlockEntity.RESULT_SLOT);
    }

    private void mapSlot(ContainerSlotType type, int slot) {
        this.addMapping(new ContainerMapping(type, this.furnaceView, slot, 1, 0));
    }
}
