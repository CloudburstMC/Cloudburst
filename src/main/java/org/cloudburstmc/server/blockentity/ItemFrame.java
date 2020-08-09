package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.server.item.Item;

public interface ItemFrame extends BlockEntity {

    int getItemRotation();

    void setItemRotation(int itemRotation);

    Item getItem();

    void setItem(Item item);

    float getItemDropChance();

    void setItemDropChance(float itemDropChance);

    int getAnalogOutput();
}
