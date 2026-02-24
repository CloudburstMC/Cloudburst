package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.AnvilScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.AnvilView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudAnvilView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the anvil. Slots are ephemeral (no block entity).
 */
public class CloudAnvilContainerScreen extends CloudBlockContainerScreen implements AnvilScreen {

    private final CloudAnvilView anvilSection;

    public CloudAnvilContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.ANVIL, player, block);
        this.anvilSection = new CloudAnvilView();
    }

    @Override
    public AnvilView getAnvil() {
        return anvilSection;
    }

    @Override
    protected Container getStorageContainer() {
        return anvilSection.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new ContainerMapping(ContainerSlotType.ANVIL_INPUT, anvilSection, 1, 0));
        this.addMapping(new ContainerMapping(ContainerSlotType.ANVIL_MATERIAL, anvilSection, 1, 1));
        this.addMapping(new ContainerMapping(ContainerSlotType.CREATED_OUTPUT, anvilSection, 1, -48));
    }
}
