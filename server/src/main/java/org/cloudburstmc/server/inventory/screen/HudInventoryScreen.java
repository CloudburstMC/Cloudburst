package org.cloudburstmc.server.inventory.screen;

import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Collection;

public class HudInventoryScreen extends CloudInventoryScreen {
    public HudInventoryScreen(CloudPlayer player) {
        super(player);
    }

    @Override
    protected void setupViews() {

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
