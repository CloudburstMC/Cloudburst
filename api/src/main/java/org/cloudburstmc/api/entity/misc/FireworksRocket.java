package org.cloudburstmc.api.entity.misc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.FireworkData;

public interface FireworksRocket extends Projectile {

    int getLife();

    void setLife(int life);

    int getLifetime();

    void setLifetime(int lifetime);

    FireworkData getFireworkData();

    void setFireworkData(@Nullable FireworkData tag);

    @Nullable
    Player getBoostedPlayer();

    void setBoostedPlayer(@Nullable Player player);
}
