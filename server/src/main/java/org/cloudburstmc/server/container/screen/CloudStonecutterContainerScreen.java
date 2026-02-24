package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.StonecutterScreen;
import org.cloudburstmc.api.inventory.view.StonecutterView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudStonecutterView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the stonecutter. All slots are ephemeral.
 */
public class CloudStonecutterContainerScreen extends CloudBlockContainerScreen implements StonecutterScreen {

    private final CloudStonecutterView stonecutter;

    public CloudStonecutterContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.STONECUTTER, player, block);
        this.stonecutter = new CloudStonecutterView(new CloudContainer(2));
    }

    @Override
    public StonecutterView getStonecutter() {
        return stonecutter;
    }

    @Override
    protected Container getStorageContainer() {
        return stonecutter.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new ContainerMapping(ContainerSlotType.STONECUTTER_INPUT, stonecutter, 1, 0));
        this.addMapping(new ContainerMapping(ContainerSlotType.STONECUTTER_RESULT, stonecutter, 1, 1));
    }
}
