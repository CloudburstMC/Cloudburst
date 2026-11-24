package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.inventory.EntityStorageScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.StorageView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.view.CloudEntityStorageView;
import org.cloudburstmc.server.entity.component.EntityStorage;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudEntityStorageScreen extends CloudContainerScreen implements EntityStorageScreen {

    private final EntityStorage storageOwner;
    private final CloudEntityStorageView storage;

    public CloudEntityStorageScreen(CloudPlayer player, EntityStorage storageOwner) {
        super(ScreenTypes.ENTITY_STORAGE, player);
        this.storageOwner = storageOwner;
        this.storage = storageOwner.getInventory();
    }

    @Override
    public Entity getEntity() {
        return this.storageOwner.getEntity();
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
        if (this.storageOwner.isUnavailableTo(this.player)) {
            throw new IllegalStateException("Entity inventory is no longer accessible");
        }

        ContainerOpenPacket packet = new ContainerOpenPacket();
        packet.setType(this.storageOwner.getWindowType());
        packet.setBlockPosition(this.getEntity().getPosition().toInt());
        packet.setUniqueEntityId(this.getEntity().getUniqueId());
        this.openWindow(packet, this.storage.getContainer());
        this.player.getInventoryManager().sendAllInventories();
        this.storageOwner.addViewer(this.player);
    }

    @Override
    public void close() {
        this.storageOwner.removeViewer(this.player);
    }
}
