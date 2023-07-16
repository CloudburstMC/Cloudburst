package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.container.ContainerScreenType;
import org.cloudburstmc.api.container.ContainerViewTypes;
import org.cloudburstmc.api.container.screen.UiContainerScreen;
import org.cloudburstmc.api.container.view.CursorView;
import org.cloudburstmc.api.container.view.InventoryView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudCursorView;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudUiContainerScreen extends CloudContainerScreen implements UiContainerScreen {

    protected final CloudCursorView cursor;

    public CloudUiContainerScreen(ContainerScreenType<?> type, CloudPlayer player) {
        super(type, player);
        this.cursor = new CloudCursorView(player);
    }

    @Override
    public InventoryView getInventory() {
        return getViewOrThrow(ContainerViewTypes.INVENTORY);
    }

    @Override
    public CursorView getCursor() {
        return getViewOrThrow(ContainerViewTypes.CURSOR);
    }

    @Override
    protected void setupMappings() {
        this.addMapping(SimpleContainerMapping.playerInventoryView(this.player.getInventory()));
        this.addMapping(new UIContainerMapping(ContainerSlotType.CURSOR, this.cursor));
    }
}
