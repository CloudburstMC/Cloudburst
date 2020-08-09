package org.cloudburstmc.server.entity.misc;

import com.nukkitx.nbt.NbtMap;
import org.cloudburstmc.server.entity.Entity;

public interface FireworksRocket extends Entity {

    int getLife();

    void setLife(int life);

    int getLifetime();

    void setLifetime(int lifetime);

    NbtMap getFireworkData();

    void setFireworkData(NbtMap tag);
}
