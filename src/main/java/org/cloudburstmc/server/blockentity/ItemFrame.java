package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.server.item.ItemStack;

public interface ItemFrame extends BlockEntity {

    int getItemRotation();

    void setItemRotation(int itemRotation);

    ItemStack getItem();

    void setItem(ItemStack item);

    float getItemDropChance();

    void setItemDropChance(float itemDropChance);

    int getAnalogOutput();
}
