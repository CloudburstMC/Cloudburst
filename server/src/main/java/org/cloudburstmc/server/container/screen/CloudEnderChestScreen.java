package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.container.ContainerScreenTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.view.CloudEnderChestView;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudEnderChestScreen extends CloudUiContainerScreen {

    private final CloudEnderChestView enderChest;

    public CloudEnderChestScreen(CloudPlayer player, Block block) {
        super(ContainerScreenTypes.ENDER_CHEST, player);
        this.enderChest = new CloudEnderChestView(player, block);
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();

        this.addMapping(new SimpleContainerMapping(ContainerSlotType.INVENTORY, this.enderChest));
    }
}
