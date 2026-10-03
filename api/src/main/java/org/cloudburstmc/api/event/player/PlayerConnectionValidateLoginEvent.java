package org.cloudburstmc.api.event.player;

import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.player.PlayerClientInfo;
import org.cloudburstmc.api.player.PlayerProfile;

import java.net.InetSocketAddress;

/**
 * Called on the server thread after pre-login and resource pack negotiation.
 * No player exists yet. Listeners may override the result of the server's ban,
 * allow-list and player-limit checks.
 */
public class PlayerConnectionValidateLoginEvent extends Event {

    private final PlayerProfile profile;
    private final PlayerClientInfo clientInfo;
    private final InetSocketAddress address;
    private PlayerLoginResult loginResult;
    private Component kickMessage;

    /**
     * Creates an admission event with the server's current decision.
     *
     * @param profile    the connecting profile
     * @param clientInfo the reported client settings
     * @param address    the remote game connection address
     * @param result     the initial admission result
     * @param message    the initial denial message, ignored when admission is allowed
     */
    public PlayerConnectionValidateLoginEvent(@NonNull PlayerProfile profile, @NonNull PlayerClientInfo clientInfo,
                                              @NonNull InetSocketAddress address, @NonNull PlayerLoginResult result,
                                              @NonNull Component message) {
        this.profile = profile;
        this.clientInfo = clientInfo;
        this.address = address;
        if (result == PlayerLoginResult.ALLOWED) {
            this.allow();
        } else {
            this.disallow(result, message);
        }
    }

    /**
     * Returns the profile requesting admission.
     *
     * @return the connecting profile
     */
    public PlayerProfile getProfile() {
        return this.profile;
    }

    /**
     * Returns the settings reported by the connecting client.
     *
     * @return the client information
     */
    public PlayerClientInfo getClientInfo() {
        return this.clientInfo;
    }

    /**
     * Returns the remote game connection address.
     *
     * @return the remote address
     */
    public InetSocketAddress getAddress() {
        return this.address;
    }

    /**
     * Returns the current admission decision.
     *
     * @return the login result
     */
    public PlayerLoginResult getLoginResult() {
        return this.loginResult;
    }

    /**
     * Returns the message shown when admission is denied.
     *
     * @return the denial message, or an empty component when allowed
     */
    public Component kickMessage() {
        return this.kickMessage;
    }

    /**
     * Allows admission and clears the denial message.
     */
    public void allow() {
        this.loginResult = PlayerLoginResult.ALLOWED;
        this.kickMessage = Component.empty();
    }

    /**
     * Denies admission with a reason and the message to show to the client.
     *
     * @param result  a result other than ALLOWED
     * @param message the denial message
     * @throws IllegalArgumentException if the result allows admission
     */
    public void disallow(@NonNull PlayerLoginResult result, @NonNull Component message) {
        if (result == PlayerLoginResult.ALLOWED) {
            throw new IllegalArgumentException("A denied login requires a kick result");
        }

        this.loginResult = result;
        this.kickMessage = message;
    }
}
