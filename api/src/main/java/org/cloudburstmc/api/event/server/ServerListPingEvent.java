package org.cloudburstmc.api.event.server;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.GameMode;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Called asynchronously for each server status request.
 * Changes affect the response to this request, not gameplay or connection limits.
 * Cancelling suppresses the response without preventing the client from joining.
 *
 * <p>Listeners must not block or access mutable gameplay state.
 * Complete all changes before returning from the listener.</p>
 */
public class ServerListPingEvent extends ServerEvent implements Cancellable {

    private final String hostname;
    private final InetSocketAddress address;

    private Component motd;
    private String levelName;
    private GameMode gameMode;
    private int playerCount;
    private int maxPlayerCount;
    private boolean hardcore;

    /**
     * Creates an event with the supplied advertised status.
     *
     * @param hostname       the untrusted requested host
     * @param address        the remote requester address
     * @param motd           the message of the day
     * @param levelName      the level name
     * @param gameMode       the game mode
     * @param playerCount    the player count, which may exceed the maximum
     * @param maxPlayerCount the maximum player count
     * @param hardcore       whether the server is advertised as hardcore
     * @throws NullPointerException     if any reference argument is null
     * @throws IllegalArgumentException if either player count is negative
     */
    public ServerListPingEvent(String hostname, InetSocketAddress address, Component motd, String levelName, GameMode gameMode, int playerCount, int maxPlayerCount, boolean hardcore) {
        super(true);
        this.hostname = Objects.requireNonNull(hostname, "hostname");
        this.address = Objects.requireNonNull(address, "address");
        this.motd(motd);
        this.setLevelName(levelName);
        this.setGameMode(gameMode);
        this.setPlayerCount(playerCount);
        this.setMaxPlayerCount(maxPlayerCount);
        this.setHardcore(hardcore);
    }

    /**
     * Returns the untrusted requested host, including a port when supplied.
     *
     * @return the requested host, or an empty string when absent
     */
    public String getHostname() {
        return this.hostname;
    }

    /**
     * Returns the remote address requesting status.
     *
     * @return the requester address
     */
    public InetSocketAddress getAddress() {
        return this.address;
    }

    /**
     * Returns the advertised message of the day.
     *
     * @return the message of the day
     */
    public Component motd() {
        return this.motd;
    }

    /**
     * Sets the advertised message of the day.
     *
     * @param motd the message of the day
     * @throws NullPointerException if the message is null
     */
    public void motd(Component motd) {
        this.motd = Objects.requireNonNull(motd, "motd");
    }

    /**
     * Returns the advertised level name.
     *
     * @return the level name
     */
    public String getLevelName() {
        return this.levelName;
    }

    /**
     * Sets the advertised level name.
     *
     * @param levelName the level name
     * @throws NullPointerException if the level name is null
     */
    public void setLevelName(String levelName) {
        this.levelName = Objects.requireNonNull(levelName, "levelName");
    }

    /**
     * Returns the advertised game mode.
     *
     * @return the game mode
     */
    public GameMode getGameMode() {
        return this.gameMode;
    }

    /**
     * Sets the advertised game mode.
     *
     * @param gameMode the game mode
     * @throws NullPointerException if the game mode is null
     */
    public void setGameMode(GameMode gameMode) {
        this.gameMode = Objects.requireNonNull(gameMode, "gameMode");
    }

    /**
     * Returns the advertised player count.
     *
     * @return the non-negative player count
     */
    public int getPlayerCount() {
        return this.playerCount;
    }

    /**
     * Sets the advertised player count, which may exceed the advertised maximum.
     *
     * @param playerCount the player count
     * @throws IllegalArgumentException if the player count is negative
     */
    public void setPlayerCount(int playerCount) {
        if (playerCount < 0) {
            throw new IllegalArgumentException("playerCount must not be negative");
        }

        this.playerCount = playerCount;
    }

    /**
     * Returns the advertised maximum player count.
     *
     * @return the non-negative maximum player count
     */
    public int getMaxPlayerCount() {
        return this.maxPlayerCount;
    }

    /**
     * Sets the advertised maximum player count.
     * The maximum may be lower than the advertised player count.
     *
     * @param maxPlayerCount the maximum player count
     * @throws IllegalArgumentException if the maximum player count is negative
     */
    public void setMaxPlayerCount(int maxPlayerCount) {
        if (maxPlayerCount < 0) {
            throw new IllegalArgumentException("maxPlayerCount must not be negative");
        }

        this.maxPlayerCount = maxPlayerCount;
    }

    /**
     * Returns whether the server is advertised as hardcore.
     *
     * @return whether the advertised status is hardcore
     */
    public boolean isHardcore() {
        return this.hardcore;
    }

    /**
     * Sets the advertised hardcore flag.
     *
     * @param hardcore whether the server is advertised as hardcore
     */
    public void setHardcore(boolean hardcore) {
        this.hardcore = hardcore;
    }
}
