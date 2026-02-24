package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.BeaconScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.BlockBeaconView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudBeaconView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the beacon. Payment slot is ephemeral (the beacon block entity
 * has no item storage — the server processes the payment when the player submits the form).
 */
public class CloudBeaconContainerScreen extends CloudBlockContainerScreen implements BeaconScreen {

    private final CloudBeaconView beaconSection;

    public CloudBeaconContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.BEACON, player, block);
        this.beaconSection = new CloudBeaconView(block);
    }

    @Override
    public BlockBeaconView getBeacon() {
        return beaconSection;
    }

    @Override
    protected Container getStorageContainer() {
        return beaconSection.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new ContainerMapping(ContainerSlotType.BEACON_PAYMENT, beaconSection, 1, -27));
    }
}
