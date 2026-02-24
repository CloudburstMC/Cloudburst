package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.BrewingStandScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.BlockBrewingStandView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.blockentity.ContainerBlockEntity;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudBrewingStandView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the brewing stand block container.
 */
public class CloudBrewingContainerScreen extends CloudBlockContainerScreen implements BrewingStandScreen {

    private final ContainerBlockEntity brewingEntity;
    private CloudBrewingStandView brewingStandView;

    public CloudBrewingContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.BREWING_STAND, player, block);
        this.brewingEntity = getOrCreateBlockEntity(block, BlockEntityTypes.BREWING_STAND);
    }

    @Override
    protected Container getStorageContainer() {
        return brewingEntity.getContainer();
    }

    @Override
    public BlockBrewingStandView getBrewingStand() {
        if (brewingStandView == null) {
            throw new IllegalStateException("getBrewingStand() called before setup(), screen mappings have not been initialised yet");
        }
        return brewingStandView;
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.brewingStandView = new CloudBrewingStandView(getBlock(), brewingEntity.getContainer());
        this.addMapping(new ContainerMapping(ContainerSlotType.BREWING_INPUT, brewingStandView, 1, 0));
        this.addMapping(new ContainerMapping(ContainerSlotType.BREWING_RESULT, brewingStandView, 3, 1));
        this.addMapping(new ContainerMapping(ContainerSlotType.BREWING_FUEL, brewingStandView, 1, 4));
    }
}
