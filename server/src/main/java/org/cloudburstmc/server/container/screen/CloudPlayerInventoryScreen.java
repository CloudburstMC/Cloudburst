package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.inventory.PlayerInventoryScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.mapping.LimitedContainerMapping;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudCraftingView;
import org.cloudburstmc.server.container.view.CloudCreatedOutputView;
import org.cloudburstmc.server.container.view.CloudCursorView;
import org.cloudburstmc.server.container.view.CloudPlayerInventory;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Screen implementation for the player's own inventory view: includes the 2×2 crafting grid,
 * armor slots, cursor, and the full 36-slot player inventory.
 */
public class CloudPlayerInventoryScreen extends CloudInventoryScreen implements PlayerInventoryScreen {

    private final CloudCursorView cursor;
    private final CloudCraftingView craftingGrid;

    public CloudPlayerInventoryScreen(CloudPlayer player) {
        super(ScreenTypes.INVENTORY, player);
        this.cursor = new CloudCursorView(player);
        this.craftingGrid = new CloudCraftingView(player);
    }

    @Override
    protected void setupMappings() {
        CloudPlayerInventory inventoryView = (CloudPlayerInventory) this.player.getInventory();
        this.addMapping(SimpleContainerMapping.forPlayerInventory(inventoryView));
        this.addMapping(new LimitedContainerMapping(ContainerSlotType.HOTBAR, inventoryView, 9));
        this.addMapping(new LimitedContainerMapping(ContainerSlotType.HOTBAR_AND_INVENTORY, inventoryView, 36));
        this.addMapping(new UIContainerMapping(ContainerSlotType.CURSOR, this.cursor));
        this.addMapping(new ContainerMapping(ContainerSlotType.CREATED_OUTPUT, new CloudCreatedOutputView(this.player), 1, -50));
        this.addMapping(new ContainerMapping(ContainerSlotType.CRAFTING_INPUT, this.craftingGrid, 4, 0));
        this.addMapping(SimpleContainerMapping.armorView(this.player.getArmor()));
        this.addMapping(SimpleContainerMapping.offhandView(this.player.getOffhand()));
    }

    @Override
    public PlayerInventoryView getInventory() {
        return getSlotsOrThrow(SlotGroupTypes.INVENTORY);
    }

    @Override
    public CraftingView getCraftingGrid() {
        return craftingGrid;
    }

    @Override
    public ArmorView getArmor() {
        return player.getArmor();
    }

    @Override
    public OffhandView getOffhand() {
        return player.getOffhand();
    }

    @Override
    public CursorView getCursor() {
        return getSlotsOrThrow(SlotGroupTypes.CURSOR);
    }
}
