package org.cloudburstmc.server.inventory.screen;

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.inventory.view.InventoryView;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.HashMap;
import java.util.Map;

public abstract class InventoryScreen {

    private final Map<ContainerSlotType, InventoryView> views = new HashMap<>();
    protected final CloudPlayer player;

    public InventoryScreen(CloudPlayer player) {
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
}
