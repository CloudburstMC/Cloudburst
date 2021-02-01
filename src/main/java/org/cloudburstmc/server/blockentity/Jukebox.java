package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.item.ItemStack;

public interface Jukebox extends BlockEntity {

    ItemStack getRecordItem();

    void setRecordItem(ItemStack recordItem);

    void play();

    void stop();

    void dropItem();
}
