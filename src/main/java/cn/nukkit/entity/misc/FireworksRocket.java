package cn.nukkit.entity.misc;

import cn.nukkit.entity.Entity;
import com.nukkitx.nbt.NbtMap;

public interface FireworksRocket extends Entity {

    int getLife();

    void setLife(int life);

    int getLifetime();

    void setLifetime(int lifetime);

    NbtMap getFireworkData();

    void setFireworkData(NbtMap tag);
}
