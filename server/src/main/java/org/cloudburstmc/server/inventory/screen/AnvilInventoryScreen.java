package org.cloudburstmc.server.inventory.screen;

import org.cloudburstmc.api.inventory.AnvilInventory;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.inventory.view.SimpleInventoryView;
import org.cloudburstmc.server.inventory.view.UIInventoryView;
import org.cloudburstmc.server.player.CloudPlayer;

public class AnvilInventoryScreen extends InventoryScreen {

    private final AnvilInventory inventory;

    public AnvilInventoryScreen(CloudPlayer player, AnvilInventory inventory) {
        super(player);
        this.inventory = inventory;
    }

    @Override
    protected void setupViews() {
        this.addView(new UIInventoryView(ContainerSlotType.CURSOR, player.getCursorInventory()));
        this.addView(new UIInventoryView(ContainerSlotType.ANVIL_INPUT, this.inventory));
        this.addView(new UIInventoryView(ContainerSlotType.ANVIL_MATERIAL, this.inventory));
        this.addView(SimpleInventoryView.playerInventoryView(this.player));
    }
}
