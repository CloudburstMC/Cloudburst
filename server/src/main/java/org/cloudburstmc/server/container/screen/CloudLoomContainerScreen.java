package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.LoomScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.LoomView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudLoomView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the loom. All slots are ephemeral.
 */
public class CloudLoomContainerScreen extends CloudBlockContainerScreen implements LoomScreen {

    private CloudLoomView loomView;

    public CloudLoomContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.LOOM, player, block);
    }

    @Override
    public LoomView getLoom() {
        return loomView;
    }

    @Override
    protected Container getStorageContainer() {
        return loomView.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.loomView = new CloudLoomView(new CloudContainer(4));
        this.addMapping(new ContainerMapping(ContainerSlotType.LOOM_INPUT, loomView, 1, 0));
        this.addMapping(new ContainerMapping(ContainerSlotType.LOOM_DYE, loomView, 1, 1));
        this.addMapping(new ContainerMapping(ContainerSlotType.LOOM_MATERIAL, loomView, 1, 2));
        this.addMapping(new ContainerMapping(ContainerSlotType.LOOM_RESULT, loomView, 1, 3));
    }
}
