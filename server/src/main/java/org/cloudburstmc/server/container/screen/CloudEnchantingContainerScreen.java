package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.inventory.EnchantingScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.EnchantingView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudEnchantingView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the enchanting table. Slots are ephemeral (no block entity).
 */
public class CloudEnchantingContainerScreen extends CloudBlockContainerScreen implements EnchantingScreen {

    private final CloudEnchantingView enchantSection;

    public CloudEnchantingContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.ENCHANTING, player, block);
        this.enchantSection = new CloudEnchantingView();
    }

    @Override
    public EnchantingView getEnchanting() {
        return enchantSection;
    }

    @Override
    protected Container getStorageContainer() {
        return enchantSection.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new ContainerMapping(ContainerSlotType.ENCHANTING_INPUT, enchantSection, 1, 0));
        this.addMapping(new ContainerMapping(ContainerSlotType.ENCHANTING_MATERIAL, enchantSection, 1, 1));
    }
}
