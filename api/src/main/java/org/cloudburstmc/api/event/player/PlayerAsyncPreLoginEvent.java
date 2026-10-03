package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.player.PlayerClientInfo;
import org.cloudburstmc.api.player.PlayerProfile;
import org.cloudburstmc.api.player.skin.Skin;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Called asynchronously after a player's login data has been validated.
 * A {@link Player} does not exist while this event is being handled. Use
 * {@link PlayerJoinEvent} for work that requires the joined player.
 * Listeners must not access mutable gameplay state and must complete changes before returning.
 */
public class PlayerAsyncPreLoginEvent extends Event {

    private final PlayerProfile profile;
    private final PlayerClientInfo clientInfo;
    private final InetSocketAddress address;

    private Skin skin;
    private PlayerLoginResult loginResult = PlayerLoginResult.ALLOWED;
    private Component kickMessage = Component.empty();

    /**
     * Creates a pre-login event for a validated connection.
     *
     * @param profile    the player profile
     * @param clientInfo the reported client settings
     * @param address    the remote network address
     * @param skin       the skin to use when login succeeds
     */
    public PlayerAsyncPreLoginEvent(PlayerProfile profile, PlayerClientInfo clientInfo, InetSocketAddress address, Skin skin) {
        super(true);
        this.profile = Objects.requireNonNull(profile, "profile");
        this.clientInfo = Objects.requireNonNull(clientInfo, "clientInfo");
        this.address = Objects.requireNonNull(address, "address");
        this.skin = Objects.requireNonNull(skin, "skin");
    }

    /**
     * Returns the profile requesting to join.
     *
     * @return the player profile
     */
    public PlayerProfile getProfile() {
        return profile;
    }

    /**
     * Returns the settings reported by the connecting client.
     *
     * @return the client information
     */
    public PlayerClientInfo getClientInfo() {
        return clientInfo;
    }

    /**
     * Returns the remote address of the connecting client.
     *
     * @return the remote address
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * Returns the skin that will be used if login succeeds.
     *
     * @return the login skin
     */
    public Skin getSkin() {
        return skin;
    }

    /**
     * Replaces the skin that will be used if login succeeds.
     *
     * @param skin the new login skin
     */
    public void setSkin(Skin skin) {
        this.skin = Objects.requireNonNull(skin, "skin");
    }

    /**
     * Returns the current login result.
     *
     * @return the login result
     */
    public PlayerLoginResult getLoginResult() {
        return loginResult;
    }

    /**
     * Returns the message shown when login is denied.
     *
     * @return the kick message
     */
    public Component kickMessage() {
        return kickMessage;
    }

    /**
     * Allows the connection to continue.
     */
    public void allow() {
        this.loginResult = PlayerLoginResult.ALLOWED;
        this.kickMessage = Component.empty();
    }

    /**
     * Denies the connection with the supplied message.
     *
     * @param result  the reason login was denied
     * @param message the message shown to the client
     */
    public void disallow(PlayerLoginResult result, Component message) {
        if (result == PlayerLoginResult.ALLOWED) {
            throw new IllegalArgumentException("A denied login requires a kick result");
        }

        this.loginResult = Objects.requireNonNull(result, "result");
        this.kickMessage = Objects.requireNonNull(message, "message");
    }
}
