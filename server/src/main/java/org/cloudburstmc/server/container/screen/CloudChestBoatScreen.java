package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.inventory.EntityStorageScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.StorageView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.view.CloudEntityStorageView;
import org.cloudburstmc.server.entity.vehicle.EntityChestBoat;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudChestBoatScreen extends CloudContainerScreen implements EntityStorageScreen {

    private final EntityChestBoat boat;
    private final CloudEntityStorageView storage;

    public CloudChestBoatScreen(CloudPlayer player, EntityChestBoat boat) {
        super(ScreenTypes.ENTITY_STORAGE, player);
        this.boat = boat;
        this.storage = (CloudEntityStorageView) boat.getInventory();
    }

    @Override
    public Entity getEntity() {
        return this.boat;
    }

    @Override
    public StorageView getStorage() {
        return this.storage;
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new SimpleContainerMapping(ContainerSlotType.LEVEL_ENTITY, this.storage));
    }

    @Override
    public void open() {
        if (this.boat.isInventoryUnavailable(this.player)) {
            throw new IllegalStateException("Chest boat inventory is no longer accessible");
        }

        ContainerOpenPacket packet = new ContainerOpenPacket();
        packet.setId(this.player.assignContainerId(this.storage.getContainer()));
        packet.setType(ContainerType.CONTAINER);
        packet.setBlockPosition(this.boat.getPosition().toInt());
        packet.setUniqueEntityId(this.boat.getUniqueId());
        this.openWindow(packet);
        this.player.getInventoryManager().sendAllInventories();
        this.boat.addInventoryViewer(this.player);
    }

    @Override
    public void close() {
        this.boat.removeInventoryViewer(this.player);
    }
}
