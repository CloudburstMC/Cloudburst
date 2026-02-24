package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.FurnaceScreen;
import org.cloudburstmc.api.inventory.ScreenType;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.BlockFurnaceView;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.blockentity.ContainerBlockEntity;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudFurnaceView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for furnace-like block containers: furnace, blast furnace, and smoker.
 */
public class CloudFurnaceContainerScreen extends CloudBlockContainerScreen implements FurnaceScreen {

    private final ContainerBlockEntity furnaceEntity;
    private final ContainerSlotType ingredientSlotType;
    private final SlotGroupType<BlockFurnaceView> slotGroupType;
    private CloudFurnaceView furnaceView;

    private CloudFurnaceContainerScreen(ScreenType<FurnaceScreen> type, CloudPlayer player, Block block,
                                        ContainerSlotType ingredientSlotType,
                                        SlotGroupType<BlockFurnaceView> slotGroupType,
                                        org.cloudburstmc.api.blockentity.BlockEntityType<?> beType) {
        super(type, player, block);
        this.furnaceEntity = getOrCreateBlockEntity(block, beType);
        this.ingredientSlotType = ingredientSlotType;
        this.slotGroupType = slotGroupType;
    }

    public static CloudFurnaceContainerScreen furnace(CloudPlayer player, Block block) {
        return new CloudFurnaceContainerScreen(ScreenTypes.FURNACE, player, block,
                ContainerSlotType.FURNACE_INGREDIENT, SlotGroupTypes.FURNACE, BlockEntityTypes.FURNACE);
    }

    public static CloudFurnaceContainerScreen blastFurnace(CloudPlayer player, Block block) {
        return new CloudFurnaceContainerScreen(ScreenTypes.BLAST_FURNACE, player, block,
                ContainerSlotType.BLAST_FURNACE_INGREDIENT, SlotGroupTypes.BLAST_FURNACE, BlockEntityTypes.BLAST_FURNACE);
    }

    public static CloudFurnaceContainerScreen smoker(CloudPlayer player, Block block) {
        return new CloudFurnaceContainerScreen(ScreenTypes.SMOKER, player, block,
                ContainerSlotType.SMOKER_INGREDIENT, SlotGroupTypes.SMOKER, BlockEntityTypes.SMOKER);
    }

    @Override
    protected Container getStorageContainer() {
        return furnaceEntity.getContainer();
    }

    @Override
    public BlockFurnaceView getFurnace() {
        return furnaceView;
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.furnaceView = new CloudFurnaceView(slotGroupType, getBlock(), furnaceEntity.getContainer());
        this.addMapping(new ContainerMapping(ingredientSlotType, furnaceView, 1, 0));
        this.addMapping(new ContainerMapping(ContainerSlotType.FURNACE_FUEL, furnaceView, 1, 1));
        this.addMapping(new ContainerMapping(ContainerSlotType.FURNACE_RESULT, furnaceView, 1, 2));
    }
}
