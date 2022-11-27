package org.cloudburstmc.server.player.handler;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.event.player.PlayerAsyncPreLoginEvent;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.handler.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.event.player.PlayerPreLoginEvent;
import org.cloudburstmc.server.network.BedrockInterface;
import org.cloudburstmc.server.network.ProtocolInfo;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.player.PlayerLoginData;
import org.cloudburstmc.server.scheduler.AsyncTask;
import org.cloudburstmc.server.utils.ClientChainData;
import org.cloudburstmc.server.utils.TextFormat;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Extollite
 */
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
    public boolean handle(LoginPacket packet) {
        int protocolVersion = packet.getProtocolVersion();
        BedrockPacketCodec packetCodec = ProtocolInfo.getPacketCodec(protocolVersion);

        if (packetCodec == null) {
            PlayStatusPacket statusPacket = new PlayStatusPacket();
            if (protocolVersion < ProtocolInfo.getDefaultProtocolVersion()) {
                statusPacket.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD);
            } else {
                statusPacket.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD);
            }
            session.sendPacketImmediately(statusPacket);
            return true;
        }
        session.setPacketCodec(packetCodec);

        this.loginData.setChainData(ClientChainData.read(packet));

        if (!this.loginData.getChainData().isXboxAuthed() && this.server.getConfig().isXboxAuth()) {
            session.disconnect("disconnectionScreen.notAuthenticated");
            return true;
        }

        String username = this.loginData.getChainData().getUsername();
        Matcher matcher = NAME_PATTERN.matcher(username);

        if (!matcher.matches() || username.equalsIgnoreCase("rcon") || username.equalsIgnoreCase("console")) {
            session.disconnect("disconnectionScreen.invalidName");
            return true;
        }

        this.loginData.setName(TextFormat.clean(username));

        if (!this.loginData.getChainData().getSerializedSkin().isValid()) {
            session.disconnect("disconnectionScreen.invalidSkin");
            return true;
        }

        PlayerPreLoginEvent playerPreLoginEvent = new PlayerPreLoginEvent(loginData, "Plugin reason");
        this.server.getEventManager().fire(playerPreLoginEvent);
        if (playerPreLoginEvent.isCancelled()) {
            session.disconnect(playerPreLoginEvent.getKickMessage());
            return true;
        }
        session.setPacketHandler(new ResourcePackPacketHandler(session, server, loginData));

        PlayerLoginData loginDataInstance = loginData;
        loginData.setPreLoginEventTask(new AsyncTask() {

            private PlayerAsyncPreLoginEvent e;

            @Override
            public void onRun() {
                e = new PlayerAsyncPreLoginEvent(loginDataInstance.getChainData());
                server.getEventManager().fire(e);
            }

            @Override
            public void onCompletion(CloudServer server) {
                if (!loginDataInstance.getSession().isClosed()) {
                    if (e.getLoginResult() == PlayerAsyncPreLoginEvent.LoginResult.KICK) {
                        loginDataInstance.getSession().disconnect(e.getKickMessage());
                    } else if (loginDataInstance.isShouldLogin()) {
                        try {
                            CloudPlayer player = loginDataInstance.initializePlayer();

                            for (Consumer<Player> action : e.getScheduledActions()) {
                                action.accept(player);
                            }
                        } catch (Exception e) {
                            //This will at least notify us of exceptions that were eaten in the network/protocol level
                            log.debug("Error in player initialization: {}", e.getMessage());
                        }
                    } else {
                        // Finished this before the resouce pack packets finished
                        loginDataInstance.setLoginTasks(e.getScheduledActions());
                    }
                }
            }
        });

        this.server.getScheduler().scheduleAsyncTask(null, loginData.getPreLoginEventTask());

        PlayStatusPacket statusPacket = new PlayStatusPacket();
        statusPacket.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        session.sendPacket(statusPacket);

        session.sendPacket(this.server.getPackManager().getPacksInfos());
        return true;
    }
}
