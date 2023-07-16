package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.container.ContainerScreenTypes;
import org.cloudburstmc.api.container.ContainerViewTypes;
import org.cloudburstmc.api.container.screen.HudScreen;
import org.cloudburstmc.api.container.view.HotbarView;
import org.cloudburstmc.api.container.view.OffhandView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.LimitedContainerMapping;
import org.cloudburstmc.server.container.view.CloudHotbarView;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudHudScreen extends CloudContainerScreen implements HudScreen {

    private final CloudHotbarView hotbar;

    public CloudHudScreen(CloudPlayer player) {
        super(ContainerScreenTypes.HUD, player);
        this.hotbar = new CloudHotbarView(player);
    }

    @Override
    protected void setupMappings() {
        this.addMapping(new LimitedContainerMapping(ContainerSlotType.HOTBAR_AND_INVENTORY, this.hotbar, 9));
    }

    @Override
    public HotbarView getHotbar() {
        return getViewOrThrow(ContainerViewTypes.HOTBAR);
    }

    @Override
    public OffhandView getOffhand() {
        return getViewOrThrow(ContainerViewTypes.OFFHAND);
    }
}
