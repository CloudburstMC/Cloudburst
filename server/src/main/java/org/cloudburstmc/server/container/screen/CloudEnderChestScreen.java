package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.EnderChestScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.EnderChestView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.view.CloudEnderChestView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for an ender chest. Backed by the player's own personal 27-slot
 * ender chest container rather than a shared block-entity container.
 */
public class CloudEnderChestScreen extends CloudBlockContainerScreen implements EnderChestScreen {

    private final CloudEnderChestView enderChest;

    public CloudEnderChestScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.ENDER_CHEST, player, block);
        this.enderChest = new CloudEnderChestView(player);
    }

    @Override
    protected Container getStorageContainer() {
        return enderChest.getContainer();
    }

    @Override
    public EnderChestView getEnderChest() {
        return getSlotsOrThrow(SlotGroupTypes.ENDER_CHEST);
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new SimpleContainerMapping(ContainerSlotType.LEVEL_ENTITY, this.enderChest));
    }
}
