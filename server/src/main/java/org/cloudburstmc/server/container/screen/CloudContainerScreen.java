package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.inventory.ContainerScreen;
import org.cloudburstmc.api.inventory.ScreenType;
import org.cloudburstmc.api.inventory.view.CursorView;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.mapping.LimitedContainerMapping;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudCreatedOutputView;
import org.cloudburstmc.server.container.view.CloudCursorView;
import org.cloudburstmc.server.container.view.CloudPlayerInventory;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Base screen implementation for all open-container screens. Extends {@link CloudInventoryScreen}
 * with cursor and full player-inventory mappings shared by every container the player opens.
 */
public class CloudContainerScreen extends CloudInventoryScreen implements ContainerScreen {

    private static final int CREATED_OUTPUT_PROTOCOL_SLOT = 50;

    protected final CloudCursorView cursor;

    public CloudContainerScreen(ScreenType<?> type, CloudPlayer player) {
        super(type, player);
        this.cursor = new CloudCursorView(player);
    }

    @Override
    public PlayerInventoryView getPlayerInventory() {
        return getSlotsOrThrow(SlotGroupTypes.INVENTORY);
    }

    @Override
    public CursorView getCursor() {
        return getSlotsOrThrow(SlotGroupTypes.CURSOR);
    }

    @Override
    protected void setupMappings() {
        CloudPlayerInventory inventoryView = (CloudPlayerInventory) this.player.getInventory();
        this.addMapping(SimpleContainerMapping.forPlayerInventory(inventoryView));
        this.addMapping(new LimitedContainerMapping(ContainerSlotType.HOTBAR, inventoryView, 9));
        this.addMapping(new LimitedContainerMapping(ContainerSlotType.HOTBAR_AND_INVENTORY, inventoryView, 36));
        this.addMapping(new UIContainerMapping(ContainerSlotType.CURSOR, this.cursor));
        this.addMapping(new ContainerMapping(ContainerSlotType.CREATED_OUTPUT, new CloudCreatedOutputView(this.player), 1, -CREATED_OUTPUT_PROTOCOL_SLOT));
    }
}
