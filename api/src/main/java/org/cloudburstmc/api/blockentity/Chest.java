package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.server.inventory.ContainerInventory;

public interface Chest extends BlockEntity, ContainerBlockEntity {

    boolean isFindable();

    void setFindable(boolean findable);

    @Override
    ContainerInventory getInventory();

    boolean isPaired();

    Chest getPair();

    boolean pairWith(Chest chest);

    boolean unpair();
}
