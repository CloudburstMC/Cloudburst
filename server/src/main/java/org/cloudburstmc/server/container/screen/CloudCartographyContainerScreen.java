package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.CartographyScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.CartographyView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudCartographyView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the cartography table. All slots are ephemeral.
 */
public class CloudCartographyContainerScreen extends CloudBlockContainerScreen implements CartographyScreen {

    private final CloudCartographyView cartography;

    public CloudCartographyContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.CARTOGRAPHY, player, block);
        this.cartography = new CloudCartographyView(new CloudContainer(3));
    }

    @Override
    public CartographyView getCartography() {
        return cartography;
    }

    @Override
    protected Container getStorageContainer() {
        return cartography.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new ContainerMapping(ContainerSlotType.CARTOGRAPHY_INPUT, cartography, 1, 0));
        this.addMapping(new ContainerMapping(ContainerSlotType.CARTOGRAPHY_ADDITIONAL, cartography, 1, 1));
        this.addMapping(new ContainerMapping(ContainerSlotType.CARTOGRAPHY_RESULT, cartography, 1, 2));
    }
}
