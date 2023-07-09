package org.cloudburstmc.api.player;

import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.blockentity.EnderChest;
import org.cloudburstmc.api.entity.Creature;
import org.cloudburstmc.api.inventory.ContainerInventory;
import org.cloudburstmc.api.inventory.InventoryHolder;
import org.cloudburstmc.api.inventory.InventoryListener;
import org.cloudburstmc.api.inventory.PlayerInventory;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.OptionalLong;
import java.util.UUID;

public interface Player extends Creature, InventoryHolder, InventoryListener {
    /**
     * Checks if this player is currently online.
     *
     * @return true if they are online
     */
    boolean isOnline();

    /**
     * Returns the name of this player.
     *
     * @return Player name
     */
    String getName();

    UUID getServerId();

    /**
     * Checks if this player is banned.
     *
     * @return true if banned
     */
    boolean isBanned();

    /**
     * Sets this player to be banned or to be pardoned.
     *
     * @param value true if banned
     */
    void setBanned(boolean value);

    /**
     * Checks if this player is whitelisted.
     *
     * @return true if whitelisted
     */
    boolean isWhitelisted();

    /**
     * Adds or removes this player from the whitelist.
     *
     * @param value true if whitelisted
     */
    void setWhitelisted(boolean value);

    /**
     * Returns the {@link Server} object carrying this player.
     *
     * @return The server instance
     */
    Server getServer();

    /**
     * Returns the time this player first played on this server.
     * <p>
     * If the player has never played before, this will return 0. Otherwise, it
     * will be the amount of milliseconds since midnight, January 1, 1970 UTC.
     *
     * @return An optional with the time this player first played on this server
     */
    OptionalLong getFirstPlayed();

    /**
     * Returns the time this player last joined in this server.
     * <p>
     * If the player has never played before, this will return 0. Otherwise, it
     * will be the amount of milliseconds since midnight, January 1, 1970 UTC.
     *
     * @return An optional with the time this player last played on this server
     */
    OptionalLong getLastPlayed();

    /**
     * Checks if the player has played on this server before.
     *
     * @return true if this player has played before
     */
    boolean hasPlayedBefore();

    boolean isInsideOfWater();

    boolean isSneaking();

    boolean isSleeping();

    boolean sleepOn(Vector3i pos);

    void stopSleep();

    boolean isOnGround();

    PlayerInventory getInventory();

    ContainerInventory getEnderChestInventory();

    void setOp(boolean value);

    boolean isOp();

    void setMovementSpeed(float speed);

    float getMovementSpeed();

    Level getLevel();

    boolean isSpectator();

    boolean isCreative();

    boolean isSurvival();

    void resetInAirTicks();

    boolean isSpawned();

    GameMode getGamemode();

    String getDisplayName();

    String getXuid();

    boolean isConnected();

    default void save() {
        save(false);
    }

    void save(boolean async);

    CardinalDirection getCardinalDirection();

    boolean isAdventure();

    Location getSpawn();

    void setSpawn(Location spawn);

    Skin getSkin();

    void setSkin(Skin newSkin);

    EnderChest getViewingEnderChest();

    void setViewingEnderChest(EnderChest chest);
}
