package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.data.Firework;

public interface FireworksRocket extends Entity {

    int getLife();

    void setLife(int life);

    int getLifetime();

    void setLifetime(int lifetime);

    Firework getFireworkData();

    void setFireworkData(Firework tag);
}
