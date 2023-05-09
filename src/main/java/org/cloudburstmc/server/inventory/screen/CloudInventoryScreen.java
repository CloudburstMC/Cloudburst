package org.cloudburstmc.server.inventory.screen;

import org.cloudburstmc.api.inventory.screen.InventoryScreen;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.inventory.view.InventoryView;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public abstract class CloudInventoryScreen implements InventoryScreen {

    private final Map<ContainerSlotType, InventoryView> views = new HashMap<>();
    protected final CloudPlayer player;

    public CloudInventoryScreen(CloudPlayer player) {
        this.player = player;
    }

    public final void setup() {
        this.setupViews();
    }

    protected void addView(InventoryView view) {
        this.views.put(view.getSlotType(), view);
    }

    protected abstract void setupViews();

    public void close() {
    }

    public ItemStack getSlot(ContainerSlotType type, int slot) {
        InventoryView view = this.views.get(type);
        requireNonNull(view, "View for slot type " + type + " not found");

        int realSlot = view.getInventorySlot(slot);
        return view.getInventory().getItem(realSlot);
    }

    public void setSlot(ContainerSlotType type, int slot, ItemStack item) {
        InventoryView view = this.views.get(type);
        requireNonNull(view, "View for slot type " + type + " not found");

        int realSlot = view.getInventorySlot(slot);
        view.getInventory().setItem(realSlot, item);
    }
}
