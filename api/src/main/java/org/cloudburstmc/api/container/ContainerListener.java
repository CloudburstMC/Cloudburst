package org.cloudburstmc.api.container;

public interface ContainerListener {

    void onInventoryAdded(Container inventory);

    void onInventoryRemoved(Container inventory);

    void onInventorySlotChange(Container inventory, int slot);

    void onInventoryContentsChange(Container inventory);

    void onInventoryDataChange(Container inventory, int property, int value);
}
