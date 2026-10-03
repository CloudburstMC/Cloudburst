package org.cloudburstmc.server.player;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.event.player.PlayerConnectionValidateLoginEvent;
import org.cloudburstmc.api.event.player.PlayerLoginResult;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.network.nethernet.CloudNetherNetPeer;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Joins pre-login validation and resource pack completion before constructing the player.
 * Completion runs on the server thread. Rejection may originate on a network or login thread.
 */
@Log4j2
public class PlayerLoginContext {
    @Getter
    private final BedrockServerSession session;
    private final CloudServer server;
    private final AtomicBoolean disconnected = new AtomicBoolean();

    @Getter
    @Setter
    private AuthenticatedPlayerData playerData;
    @Getter
    @Setter
    private volatile boolean clientCacheEnabled;

    private boolean preLoginComplete;
    private boolean resourcePacksComplete;
    private boolean playerInitialized;

    public PlayerLoginContext(BedrockServerSession session, CloudServer server) {
        this.session = session;
        this.server = server;
    }

    public void completePreLogin() {
        this.requireServerThread();
        this.preLoginComplete = true;
        this.initializePlayerIfReady();
    }

    public void completeResourcePacks() {
        this.requireServerThread();
        this.resourcePacksComplete = true;
        this.initializePlayerIfReady();
    }

    public void disconnect(DisconnectFailReason reason, Component message) {
        if (this.disconnected.compareAndSet(false, true)) {
            ((CloudNetherNetPeer) this.session.getPeer()).disconnectSession(this.session, reason, message);
        }
    }

    public boolean isActive() {
        return !this.disconnected.get() && this.session.getPeer().isConnected();
    }

    private void requireServerThread() {
        if (!this.server.isPrimaryThread()) {
            throw new IllegalStateException("Login completion must run on the server thread");
        }
    }

    private void initializePlayerIfReady() {
        if (!this.preLoginComplete || !this.resourcePacksComplete || this.playerInitialized || !this.isActive()) {
            return;
        }

        this.playerInitialized = true;
        CloudPlayer player = null;
        try {
            if (!this.validateAdmission()) {
                return;
            }
            player = this.createPlayer();
            player.processLogin();
            if (!player.isConnected() || !this.isActive()) {
                return;
            }

            player.setClientCacheEnabled(this.clientCacheEnabled);
            player.completeLoginSequence();
        } catch (RuntimeException failure) {
            log.error("Failed to initialize player", failure);
            try {
                if (player != null) {
                    player.close();
                }
            } finally {
                if (player != null) {
                    this.server.removePlayer(player);
                }

                this.session.getPeer().getChannel().close();
            }
        }
    }

    private boolean validateAdmission() {
        InetSocketAddress address = (InetSocketAddress) this.session.getSocketAddress();
        Collection<CloudPlayer> onlinePlayers = this.server.getOnlinePlayers().values();
        PlayerLoginResult result = PlayerLoginResult.ALLOWED;
        Component message = Component.empty();

        if (this.server.getNameBans().isBanned(this.playerData.getName().toLowerCase(Locale.ROOT))
                || address.getAddress() != null && this.server.getIPBans().isBanned(address.getAddress().getHostAddress())) {
            result = PlayerLoginResult.KICK_BANNED;
            message = Component.text("You are banned");
        } else if (!this.server.isWhitelisted(this.playerData.getName())) {
            result = PlayerLoginResult.KICK_WHITELIST;
            message = Component.text("Server is white-listed");
        } else if (onlinePlayers.size() >= this.server.getMaxPlayers()
                && onlinePlayers.stream().noneMatch(player ->
                        player.getServerId().equals(this.playerData.getUniqueId())
                        || player.getName().equalsIgnoreCase(this.playerData.getName()))) {
            result = PlayerLoginResult.KICK_FULL;
            message = Component.translatable("disconnectionScreen.serverFull");
        }

        PlayerConnectionValidateLoginEvent event = new PlayerConnectionValidateLoginEvent(this.playerData, this.playerData, address, result, message);
        this.server.getEventManager().fire(event);
        if (!this.isActive()) {
            return false;
        }

        if (event.getLoginResult() == PlayerLoginResult.ALLOWED) {
            return true;
        }

        this.disconnect(disconnectReason(event.getLoginResult()), event.kickMessage());
        return false;
    }

    public static DisconnectFailReason disconnectReason(PlayerLoginResult result) {
        return switch (result) {
            case KICK_FULL -> DisconnectFailReason.SERVER_FULL;
            case KICK_WHITELIST -> DisconnectFailReason.NOT_ALLOWED;
            case KICK_BANNED, KICK_OTHER -> DisconnectFailReason.KICKED;
            case ALLOWED -> throw new IllegalArgumentException("Allowed login has no disconnect reason");
        };
    }

    private CloudPlayer createPlayer() {
        CloudPlayer player = new CloudPlayer(this.session, this.playerData);
        this.server.addPlayer(this.session.getSocketAddress(), player);
        return player;
    }
}
