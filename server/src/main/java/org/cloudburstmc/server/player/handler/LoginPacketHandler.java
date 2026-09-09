package org.cloudburstmc.server.player.handler;

import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.event.player.PlayerAsyncPreLoginEvent;
import org.cloudburstmc.api.event.player.PlayerLoginResult;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.network.BedrockInterface;
import org.cloudburstmc.server.player.AuthenticatedPlayerData;
import org.cloudburstmc.server.player.PlayerLoginContext;

import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class LoginPacketHandler implements BedrockPacketHandler {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private final BedrockServerSession session;
    private final CloudServer server;
    private final PlayerLoginContext loginContext;

    public LoginPacketHandler(BedrockServerSession session, CloudServer server, BedrockInterface interfaz) {
        this.session = session;
        this.server = server;
        this.loginContext = new PlayerLoginContext(session, server, interfaz);
    }

    @Override
    public PacketSignal handle(LoginPacket packet) {
        try {
            this.loginContext.setPlayerData(AuthenticatedPlayerData.read(packet));
        } catch (RuntimeException exception) {
            log.debug("Rejected invalid login data from {}", this.session.getSocketAddress(), exception);
            this.disconnect(DisconnectFailReason.NOT_AUTHENTICATED, "disconnectionScreen.notAuthenticated");
            return PacketSignal.HANDLED;
        }

        if (!this.loginContext.getPlayerData().isAuthenticated() && this.server.getConfig().isXboxAuth()) {
            this.disconnect(DisconnectFailReason.NOT_AUTHENTICATED, "disconnectionScreen.notAuthenticated");
            return PacketSignal.HANDLED;
        }

        String username = this.loginContext.getPlayerData().getName();
        Matcher matcher = NAME_PATTERN.matcher(username);

        if (!matcher.matches() || username.equalsIgnoreCase("rcon") || username.equalsIgnoreCase("console")) {
            this.disconnect(DisconnectFailReason.INVALID_NAME, "disconnectionScreen.invalidName");
            return PacketSignal.HANDLED;
        }

        if (!this.loginContext.getPlayerData().getSerializedSkin().isValid()) {
            this.disconnect(DisconnectFailReason.INVALID_PLATFORM_SKIN, "disconnectionScreen.invalidSkin");
            return PacketSignal.HANDLED;
        }

        PlayerLoginContext context = this.loginContext;
        this.server.getAsyncScheduler().runNow(null, asyncTask -> {
            PlayerAsyncPreLoginEvent event = new PlayerAsyncPreLoginEvent(
                    context.getPlayerData(), context.getPlayerData(),
                    (InetSocketAddress) context.getSession().getSocketAddress(),
                    context.getPlayerData().getSkin());
            this.server.getEventManager().fire(event);
            context.getPlayerData().setSkin(event.getSkin());

            this.server.getGlobalScheduler().run(null, mainTask -> {
                if (context.getSession().getPeer().isConnected()) {
                    if (event.getLoginResult() != PlayerLoginResult.ALLOWED) {
                        context.disconnect(disconnectReason(event.getLoginResult()), event.kickMessage());
                    } else {
                        context.completePreLogin(event.getScheduledActions());
                    }
                }
            });
        });

        EncryptionHandshakePacketHandler handshake = new EncryptionHandshakePacketHandler(this.session, this.server, this.loginContext);
        this.session.setPacketHandler(handshake);
        handshake.begin(this.loginContext.getPlayerData().getParsedIdentityPublicKey());
        return PacketSignal.HANDLED;
    }

    private void disconnect(DisconnectFailReason reason, String translationKey) {
        this.loginContext.disconnect(reason, Component.text(this.server.getLanguage().translate(translationKey)));
    }

    private static DisconnectFailReason disconnectReason(PlayerLoginResult result) {
        return switch (result) {
            case KICK_FULL -> DisconnectFailReason.SERVER_FULL;
            case KICK_WHITELIST -> DisconnectFailReason.NOT_ALLOWED;
            case KICK_BANNED, KICK_OTHER -> DisconnectFailReason.KICKED;
            case ALLOWED -> throw new IllegalArgumentException("Allowed login has no disconnect reason");
        };
    }
}
