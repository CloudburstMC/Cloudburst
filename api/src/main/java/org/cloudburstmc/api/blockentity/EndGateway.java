package org.cloudburstmc.api.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.Location;

/**
 * Represents an End gateway.
 */
public interface EndGateway extends BlockEntity {

    /**
     * Returns the location to which entities are teleported.
     *
     * @return the exit location, or {@code null} if one has not been generated
     */
    @Nullable
    Location getExitLocation();

    /**
     * Changes the location to which entities are teleported.
     *
     * @param location the exit location, or {@code null} to generate one when next used
     * @throws IllegalArgumentException if the location is in another level
     */
    void setExitLocation(@Nullable Location location);

    /**
     * @return whether entities are teleported directly to the exit location
     */
    boolean isExactTeleport();

    /**
     * Changes whether entities are teleported directly to the exit location.
     *
     * @param exactTeleport whether to use the exact exit location
     */
    void setExactTeleport(boolean exactTeleport);

    /**
     * @return the age of this gateway in ticks
     * @apiNote A gateway displays its creation beam for its first 200 ticks and
     * displays a cooldown beam every 2400 ticks.
     */
    long getAge();

    /**
     * Changes the age of this gateway.
     *
     * @param age age in ticks
     * @apiNote A gateway displays its creation beam for its first 200 ticks and
     * displays a cooldown beam every 2400 ticks.
     */
    void setAge(long age);
}
