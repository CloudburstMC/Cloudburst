package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.server.item.behavior.Item;

public interface Jukebox extends BlockEntity {

    Item getRecordItem();

    void setRecordItem(Item recordItem);

    void play();

    void stop();

    void dropItem();
}
