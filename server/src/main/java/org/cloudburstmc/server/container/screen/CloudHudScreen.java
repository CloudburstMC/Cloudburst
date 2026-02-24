package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.inventory.HudScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.HotbarView;
import org.cloudburstmc.api.inventory.view.OffhandView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.LimitedContainerMapping;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.view.CloudHotbarView;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * The always-present HUD screen shown to the player. Exposes hotbar and offhand slot mappings
 * and acts as the base layer of the player's screen stack.
 */
public class CloudHudScreen extends CloudInventoryScreen implements HudScreen {

    private final CloudHotbarView hotbar;

    public CloudHudScreen(CloudPlayer player) {
        super(ScreenTypes.HUD, player);
        this.hotbar = new CloudHotbarView(player);
    }

    @Override
    protected void setupMappings() {
        this.addMapping(new LimitedContainerMapping(ContainerSlotType.HOTBAR_AND_INVENTORY, this.hotbar, 9));
        this.addMapping(SimpleContainerMapping.offhandView(this.player.getOffhand()));
    }

    @Override
    public HotbarView getHotbar() {
        return this.hotbar;
    }

    @Override
    public OffhandView getOffhand() {
        return this.player.getOffhand();
    }
}
