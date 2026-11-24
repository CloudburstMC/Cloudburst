package org.cloudburstmc.api.entity.projectile;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.FireworkData;

/**
 * A firework projectile that can fly freely or boost a player.
 */
public interface FireworkRocket extends Projectile {

    /**
     * Returns the number of ticks the rocket has flown.
     */
    int getTicksFlown();

    /**
     * Sets the number of ticks the rocket has flown.
     */
    void setTicksFlown(int ticks);

    /**
     * Returns the flight tick threshold. The rocket detonates once it exceeds this value.
     */
    int getTicksToDetonate();

    /**
     * Sets the flight tick threshold for detonation.
     */
    void setTicksToDetonate(int ticks);

    /**
     * Returns whether the rocket follows its launch direction instead of rising.
     */
    boolean isShotAtAngle();

    /**
     * Sets whether the rocket follows its launch direction instead of rising.
     */
    void setShotAtAngle(boolean shotAtAngle);

    /**
     * Returns the rocket's effect and flight settings.
     */
    FireworkData getFireworkData();

    /**
     * Changes the rocket's firework data and recalculates its detonation threshold.
     *
     * @param fireworkData the firework data, or {@code null} to use the default
     */
    void setFireworkData(@Nullable FireworkData fireworkData);

    /**
     * Returns the player attached to this rocket for a boost, or {@code null}.
     */
    @Nullable
    Player getBoostedPlayer();

    /**
     * Attaches the rocket to a player for a boost, or detaches it with {@code null}.
     */
    void setBoostedPlayer(@Nullable Player player);
}
