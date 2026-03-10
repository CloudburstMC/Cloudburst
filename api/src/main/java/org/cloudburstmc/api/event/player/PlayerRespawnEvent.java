package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Fired just before a player is teleported to their respawn position, on both initial
 * join-spawn and death respawn.
 */
public final class PlayerRespawnEvent extends PlayerEvent {

    private final Set<RespawnFlag> flags;
    private Location location;

    public PlayerRespawnEvent(Player player, Location location, Set<RespawnFlag> flags) {
        super(player);
        this.location = location;
        this.flags = Collections.unmodifiableSet(flags.isEmpty() ? EnumSet.noneOf(RespawnFlag.class) : EnumSet.copyOf(flags));
    }

    /**
     * The location the player will be teleported to. Plugins may change this.
     */
    public Location getRespawnLocation() {
        return location;
    }

    public void setRespawnLocation(Location location) {
        this.location = location;
    }

    /**
     * Immutable set of flags describing why this respawn is happening.
     */
    public Set<RespawnFlag> getRespawnFlags() {
        return flags;
    }

    public boolean hasFlag(RespawnFlag flag) {
        return flags.contains(flag);
    }

    public enum RespawnFlag {
        /**
         * The player's respawn point is a valid, charged respawn anchor.
         */
        ANCHOR_SPAWN,
        /**
         * The player's respawn point is a valid, unobstructed bed.
         */
        BED_SPAWN,
        /**
         * The respawn was triggered by the player stepping through the end portal to return home.
         */
        END_PORTAL,
        /**
         * This is the player's very first spawn on login, not a post-death respawn.
         */
        FIRST_SPAWN
    }
}
