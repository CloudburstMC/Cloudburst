package org.cloudburstmc.server.player.handler;

import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.event.player.PlayerAsyncPreLoginEvent;
import org.cloudburstmc.api.event.player.PlayerLoginResult;
import org.cloudburstmc.netty.util.nethernet.TransportIdentityBinding;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.player.AuthenticatedPlayerData;
import org.cloudburstmc.server.player.PlayerLoginContext;

import java.net.InetSocketAddress;
import java.util.regex.Pattern;

@Log4j2
public class LoginPacketHandler implements BedrockPacketHandler {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_ ]{3,16}$");

    private final BedrockServerSession session;
    private final CloudServer server;
    private final PlayerLoginContext loginContext;
    private boolean loginStarted;

    public LoginPacketHandler(BedrockServerSession session, CloudServer server) {
        this.session = session;
        this.server = server;
        this.loginContext = new PlayerLoginContext(session, server);
    }

    @Override
    public PacketSignal handle(LoginPacket packet) {
        if (this.loginStarted) {
            this.disconnect(DisconnectFailReason.NOT_AUTHENTICATED, "disconnectionScreen.notAuthenticated");
            return PacketSignal.HANDLED;
        }

        this.loginStarted = true;

        if (!this.server.getNetwork().executeLogin(() -> this.validateLogin(packet))) {
            this.disconnect(DisconnectFailReason.SERVER_FULL, "disconnectionScreen.serverFull");
        }

        return PacketSignal.HANDLED;
    }

    @SuppressWarnings("resource")
    private void validateLogin(LoginPacket packet) {
        if (!this.loginContext.isActive()) {
            return;
        }

        try {
            this.loginContext.setPlayerData(AuthenticatedPlayerData.read(packet));
        } catch (RuntimeException | LinkageError | AssertionError exception) {
            log.debug("Rejected invalid login data from {} ({})",
                    this.session.getSocketAddress(),
                    exception.getClass().getSimpleName());
            this.disconnect(DisconnectFailReason.NOT_AUTHENTICATED, "disconnectionScreen.notAuthenticated");
            return;
        }

        if (!this.loginContext.isActive()) {
            return;
        }

        String identityMismatch = TransportIdentityBinding.mismatch(this.session.getPeer().getChannel(),
                this.loginContext.getPlayerData().getParsedIdentityPublicKey());
        if (identityMismatch != null) {
            log.debug("Rejected login identity from {}: {}", this.session.getSocketAddress(), identityMismatch);
            this.disconnect(DisconnectFailReason.NOT_AUTHENTICATED, "disconnectionScreen.notAuthenticated");
            return;
        }

        if (!this.loginContext.getPlayerData().isAuthenticated() && this.server.getConfig().isXboxAuth()) {
            this.disconnect(DisconnectFailReason.NOT_AUTHENTICATED, "disconnectionScreen.notAuthenticated");
            return;
        }

        String username = this.loginContext.getPlayerData().getName();
        if (!NAME_PATTERN.matcher(username).matches() || !username.equals(username.strip())
                || username.equalsIgnoreCase("rcon") || username.equalsIgnoreCase("console")) {
            this.disconnect(DisconnectFailReason.INVALID_NAME, "disconnectionScreen.invalidName");
            return;
        }

        if (!this.loginContext.getPlayerData().getSerializedSkin().isValid()) {
            this.disconnect(DisconnectFailReason.INVALID_PLATFORM_SKIN, "disconnectionScreen.invalidSkin");
            return;
        }

        PlayerLoginContext context = this.loginContext;
        PlayerAsyncPreLoginEvent event = new PlayerAsyncPreLoginEvent(
                context.getPlayerData(), context.getPlayerData(),
                (InetSocketAddress) context.getSession().getSocketAddress(),
                context.getPlayerData().getSkin());
        this.server.getEventManager().fire(event);
        context.getPlayerData().setSkin(event.getSkin());

        this.session.getPeer().getChannel().eventLoop().execute(() -> {
            if (!context.isActive()) {
                return;
            }

            if (event.getLoginResult() != PlayerLoginResult.ALLOWED) {
                context.disconnect(PlayerLoginContext.disconnectReason(event.getLoginResult()), event.kickMessage());
                return;
            }

            this.session.setPacketHandler(new ResourcePackPacketHandler(this.session, this.server, context));
            PlayStatusPacket statusPacket = new PlayStatusPacket();
            statusPacket.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
            this.session.sendPacket(statusPacket);
            this.session.sendPacket(this.server.getPackManager().createInfoPacket(this.server.getForceResources()));
            this.server.getGlobalScheduler().run(null, mainTask -> context.completePreLogin());
        });
    }

    @Override
    public PacketSignal handle(ClientCacheStatusPacket packet) {
        this.loginContext.setClientCacheEnabled(packet.isSupported());
        return PacketSignal.HANDLED;
    }

    private void disconnect(DisconnectFailReason reason, String translationKey) {
        this.loginContext.disconnect(reason, Component.text(this.server.getLanguage().translate(translationKey)));
    }
}
