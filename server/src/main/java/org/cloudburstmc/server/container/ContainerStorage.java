package org.cloudburstmc.server.container;

import org.cloudburstmc.api.item.ItemStack;

public interface ContainerStorage {

    ItemStack get(int slot);

    void set(int slot, ItemStack itemStack);

    int size();

    void resize(int size);

    ItemStack[] getContents();

    void setContents(ItemStack[] contents);
}
