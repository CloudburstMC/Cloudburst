package org.cloudburstmc.server.player.handler;

import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.cloudburstmc.api.event.player.PlayerAsyncPreLoginEvent;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.event.player.PlayerPreLoginEvent;
import org.cloudburstmc.server.network.BedrockInterface;
import org.cloudburstmc.server.network.ProtocolInfo;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.player.PlayerLoginData;
import org.cloudburstmc.server.utils.ClientChainData;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class LoginPacketHandler implements BedrockPacketHandler {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[aA-zZ\\s\\d_]{3,16}+$");

    private final BedrockServerSession session;
    private final CloudServer server;

    private final PlayerLoginData loginData;

    public LoginPacketHandler(BedrockServerSession session, CloudServer server, BedrockInterface interfaz) {
        this.session = session;
        this.server = server;
        this.loginData = new PlayerLoginData(session, server, interfaz);
    }

    @Override
    public PacketSignal handle(RequestNetworkSettingsPacket packet) {
        int protocolVersion = packet.getProtocolVersion();
        BedrockCodec codec = ProtocolInfo.getPacketCodec(protocolVersion);

        if (codec == null) {
            PlayStatusPacket statusPacket = new PlayStatusPacket();
            if (protocolVersion < ProtocolInfo.getDefaultProtocolVersion()) {
                statusPacket.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD);
            } else {
                statusPacket.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD);
            }
            session.sendPacketImmediately(statusPacket);
            return PacketSignal.HANDLED;
        }
        session.setCodec(codec);

        NetworkSettingsPacket networkSettings = new NetworkSettingsPacket();
        networkSettings.setCompressionThreshold(1);
        networkSettings.setCompressionAlgorithm(PacketCompressionAlgorithm.ZLIB);

        session.sendPacketImmediately(networkSettings);
        session.setCompression(PacketCompressionAlgorithm.ZLIB);

        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LoginPacket packet) {
        this.loginData.setChainData(ClientChainData.read(packet));

        if (!this.loginData.getChainData().isXboxAuthed() && this.server.getConfig().isXboxAuth()) {
            session.disconnect("disconnectionScreen.notAuthenticated");
            return PacketSignal.HANDLED;
        }

        String username = this.loginData.getChainData().getUsername();
        Matcher matcher = NAME_PATTERN.matcher(username);

        if (!matcher.matches() || username.equalsIgnoreCase("rcon") || username.equalsIgnoreCase("console")) {
            session.disconnect("disconnectionScreen.invalidName");
            return PacketSignal.HANDLED;
        }

        this.loginData.setName(PlainTextComponentSerializer.plainText().serialize(BedrockLegacyTextSerializer.getInstance().deserialize(username)));

        if (!this.loginData.getChainData().getSerializedSkin().isValid()) {
            session.disconnect("disconnectionScreen.invalidSkin");
            return PacketSignal.HANDLED;
        }

        PlayerPreLoginEvent playerPreLoginEvent = new PlayerPreLoginEvent(loginData, "Plugin reason");
        this.server.getEventManager().fire(playerPreLoginEvent);
        if (playerPreLoginEvent.isCancelled()) {
            session.disconnect(playerPreLoginEvent.getKickMessage());
            return PacketSignal.HANDLED;
        }
        session.setPacketHandler(new ResourcePackPacketHandler(session, server, loginData));

        PlayerLoginData loginDataInstance = loginData;
        loginData.setPreLoginEventTask(() ->
                server.getAsyncScheduler().runNow(null, asyncTask -> {
                    PlayerAsyncPreLoginEvent e = new PlayerAsyncPreLoginEvent(loginDataInstance.getChainData());
                    server.getEventManager().fire(e);

                    server.getGlobalScheduler().run(null, mainTask -> {
                        loginDataInstance.setPreLoginDone(true);
                        if (loginDataInstance.getSession().getPeer().isConnected()) {
                            if (e.getLoginResult() == PlayerAsyncPreLoginEvent.LoginResult.KICK) {
                                loginDataInstance.getSession().disconnect(e.getKickMessage());
                            } else if (loginDataInstance.isShouldLogin()) {
                                try {
                                    CloudPlayer player = loginDataInstance.initializePlayer();
                                    for (Consumer<Player> action : e.getScheduledActions()) {
                                        action.accept(player);
                                    }
                                } catch (Exception ex) {
                                    log.debug("Error in player initialization: {}", ex.getMessage());
                                }
                            } else {
                                loginDataInstance.setLoginTasks(e.getScheduledActions());
                            }
                        }
                    });
                })
        );

        loginData.getPreLoginEventTask().run();

        PlayStatusPacket statusPacket = new PlayStatusPacket();
        statusPacket.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        session.sendPacket(statusPacket);

        session.sendPacket(this.server.getPackManager().getPacksInfos());
        return PacketSignal.HANDLED;
    }
}
