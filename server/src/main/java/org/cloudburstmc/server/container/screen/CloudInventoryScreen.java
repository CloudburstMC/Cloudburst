package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.container.ContainerScreenTypes;
import org.cloudburstmc.api.container.screen.InventoryScreen;
import org.cloudburstmc.api.container.view.ArmorView;
import org.cloudburstmc.api.container.view.OffhandView;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudInventoryScreen extends CloudUiContainerScreen implements InventoryScreen {
    public CloudInventoryScreen(CloudPlayer player) {
        super(ContainerScreenTypes.INVENTORY, player);
    }

    @Override
    protected void setupMappings() {
        this.addMapping(SimpleContainerMapping.playerInventoryView(this.player.getInventory()));
        this.addMapping(SimpleContainerMapping.armorView(this.player.getArmor()));
        this.addMapping(SimpleContainerMapping.offhandView(this.player.getOffhand()));
    }

    @Override
    public ArmorView getArmor() {
        return player.getArmor();
    }

    @Override
    public OffhandView getOffhand() {
        return player.getOffhand();
    }
}
