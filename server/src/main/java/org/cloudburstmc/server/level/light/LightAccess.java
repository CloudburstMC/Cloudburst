package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.block.BlockState;

public interface LightAccess {

    int minHeight();

    int maxHeight();

    boolean isLoaded(int x, int z);

    BlockState primary(int x, int y, int z);

    BlockState secondary(int x, int y, int z);

    int light(int x, int y, int z);

    void setLight(int x, int y, int z, int light);

    LightStateCache states();
}
