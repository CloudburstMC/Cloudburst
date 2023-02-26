package org.cloudburstmc.server.inventory.screen;

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.inventory.view.SimpleInventoryView;
import org.cloudburstmc.server.inventory.view.UIInventoryView;
import org.cloudburstmc.server.player.CloudPlayer;

public class PlayerInventoryScreen extends InventoryScreen {
    public PlayerInventoryScreen(CloudPlayer player) {
        super(player);
    }

    @Override
    protected void setupViews() {
        this.addView(new UIInventoryView(ContainerSlotType.CURSOR, player.getCursorInventory()));
        this.addView(SimpleInventoryView.playerInventoryView(this.player));
        this.addView(SimpleInventoryView.armorView(this.player.getInventoryManager().getArmor()));
        this.addView(SimpleInventoryView.handView(this.player.getInventoryManager().getHand()));
    }
}
