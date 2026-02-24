package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.GrindstoneScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.GrindstoneView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudGrindstoneView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the grindstone. All slots are ephemeral.
 */
public class CloudGrindstoneContainerScreen extends CloudBlockContainerScreen implements GrindstoneScreen {

    private final CloudGrindstoneView grindstoneView;

    public CloudGrindstoneContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.GRINDSTONE, player, block);
        this.grindstoneView = new CloudGrindstoneView(new CloudContainer(3));
    }

    @Override
    public GrindstoneView getGrindstone() {
        return grindstoneView;
    }

    @Override
    protected Container getStorageContainer() {
        return grindstoneView.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new ContainerMapping(ContainerSlotType.GRINDSTONE_INPUT, grindstoneView, 1, 0));
        this.addMapping(new ContainerMapping(ContainerSlotType.GRINDSTONE_ADDITIONAL, grindstoneView, 1, 1));
        this.addMapping(new ContainerMapping(ContainerSlotType.GRINDSTONE_RESULT, grindstoneView, 1, 2));
    }
}
