package org.cloudburstmc.server.player;

import io.netty.channel.ChannelFutureListener;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.player.PlayerConnectionValidateLoginEvent;
import org.cloudburstmc.api.event.player.PlayerLoginResult;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.network.nethernet.CloudNetherNetPeer;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
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
    private boolean initializationStarted;

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
        if (!this.preLoginComplete || !this.resourcePacksComplete || this.initializationStarted || !this.isActive()) {
            return;
        }

        this.initializationStarted = true;
        CloudPlayer player = null;
        try {
            if (!this.validateAdmission()) {
                return;
            }

            player = this.createPlayer();
            player.loadLoginData();
            if (!player.isConnected() || !this.isActive()) {
                return;
            }

            this.prepareSpawn(player);
        } catch (RuntimeException failure) {
            this.failInitialization(player, failure);
        }
    }

    private void prepareSpawn(CloudPlayer player) {
        CompletableFuture<CloudChunk> chunkFuture = player.getLevel().getChunkFuture(
                player.getPosition().getFloorX() >> 4, player.getPosition().getFloorZ() >> 4);
        ChannelFutureListener cancelLoad = ignored -> chunkFuture.cancel(false);
        this.session.getPeer().getChannel().closeFuture().addListener(cancelLoad);
        chunkFuture.whenComplete((chunk, failure) -> {
            this.session.getPeer().getChannel().closeFuture().removeListener(cancelLoad);
            this.completeInitialization(player, failure);
        });
    }

    private void completeInitialization(CloudPlayer player, @Nullable Throwable failure) {
        if (!this.server.isPrimaryThread()) {
            this.server.getGlobalScheduler().execute(null, () -> this.completeInitialization(player, failure));
            return;
        }

        if (!this.isActive() || !player.isConnected()) {
            player.close();
            return;
        }

        if (failure != null) {
            this.failInitialization(player, failure);
            return;
        }

        try {
            player.initializeForLogin();
            player.setClientCacheEnabled(this.clientCacheEnabled);
            player.completeLoginSequence();
        } catch (RuntimeException exception) {
            this.failInitialization(player, exception);
        }
    }

    private void failInitialization(@Nullable CloudPlayer player, Throwable failure) {
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

    private boolean validateAdmission() {
        InetSocketAddress address = (InetSocketAddress) this.session.getSocketAddress();
        Collection<CloudPlayer> onlinePlayers = this.server.getOnlinePlayers().values();
        PlayerLoginResult result = PlayerLoginResult.ALLOWED;
        Component message = Component.empty();

        if (this.server.getPlayerBanList().isBanned(this.playerData.getUniqueId())
                || address.getAddress() != null && this.server.getIpBanList().isBanned(address.getAddress())) {
            result = PlayerLoginResult.KICK_BANNED;
            message = Component.text("You are banned");
        } else if (!this.server.mayJoin(this.playerData.getUniqueId())) {
            result = PlayerLoginResult.KICK_WHITELIST;
            message = Component.text("Server is white-listed");
        } else if (onlinePlayers.size() >= this.server.getMaxPlayers()
                && !this.server.canBypassPlayerLimit(this.playerData.getUniqueId())
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
