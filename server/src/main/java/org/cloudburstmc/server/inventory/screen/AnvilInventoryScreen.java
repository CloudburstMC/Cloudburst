package org.cloudburstmc.server.inventory.screen;

import org.cloudburstmc.api.inventory.AnvilInventory;
import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.inventory.view.SimpleInventoryView;
import org.cloudburstmc.server.inventory.view.UIInventoryView;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Collection;

public class AnvilInventoryScreen extends CloudInventoryScreen {

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

    @Override
    public ItemStack getItem(int slot) {
        return null;
    }

    @Override
    public void setItem(int slot, ItemStack item) {

    }

    @Override
    public Collection<Inventory> getInventories() {
        return null;
    }
}
