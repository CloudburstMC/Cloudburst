package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.SmithingScreen;
import org.cloudburstmc.api.inventory.view.SmithingView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudSmithingView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the smithing table. All slots are ephemeral.
 * View index mapping: template=0, input=1, material=2, result=3.
 */
public class CloudSmithingContainerScreen extends CloudBlockContainerScreen implements SmithingScreen {

    private final CloudSmithingView smithingView;

    public CloudSmithingContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.SMITHING, player, block);
        this.smithingView = new CloudSmithingView(new CloudContainer(4));
    }

    @Override
    public SmithingView getSmithing() {
        return smithingView;
    }

    @Override
    protected Container getStorageContainer() {
        return smithingView.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new ContainerMapping(ContainerSlotType.SMITHING_TABLE_TEMPLATE, smithingView, 1, -53));
        this.addMapping(new ContainerMapping(ContainerSlotType.SMITHING_TABLE_INPUT, smithingView, 1, -50));
        this.addMapping(new ContainerMapping(ContainerSlotType.SMITHING_TABLE_MATERIAL, smithingView, 1, -50));
        this.addMapping(new ContainerMapping(ContainerSlotType.SMITHING_TABLE_RESULT, smithingView, 1, -47));
        this.addMapping(new ContainerMapping(ContainerSlotType.CREATED_OUTPUT, smithingView, 1, -47));
    }
}
