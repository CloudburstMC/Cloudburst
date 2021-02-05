package org.cloudburstmc.api.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.util.data.FireworkData;

public interface FireworksRocket extends Entity {

    int getLife();

    void setLife(int life);

    int getLifetime();

    void setLifetime(int lifetime);

    FireworkData getFireworkData();

    void setFireworkData(FireworkData tag);
}
