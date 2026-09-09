package org.cloudburstmc.server.player.handler;

import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.ClientToServerHandshakePacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.ServerToClientHandshakePacket;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.player.PlayerLoginContext;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PublicKey;

@Log4j2
public class EncryptionHandshakePacketHandler implements BedrockPacketHandler {

    private final BedrockServerSession session;
    private final CloudServer server;
    private final PlayerLoginContext loginContext;
    private boolean awaitingResponse;

    public EncryptionHandshakePacketHandler(BedrockServerSession session, CloudServer server, PlayerLoginContext loginContext) {
        this.session = session;
        this.server = server;
        this.loginContext = loginContext;
    }

    public void begin(PublicKey clientPublicKey) {
        if (this.awaitingResponse) {
            throw new IllegalStateException("Encryption handshake has already started");
        }

        try {
            KeyPair serverKeyPair = EncryptionUtils.createKeyPair();
            byte[] salt = EncryptionUtils.generateRandomToken();
            SecretKey secretKey = EncryptionUtils.getSecretKey(serverKeyPair.getPrivate(), clientPublicKey, salt);

            ServerToClientHandshakePacket packet = new ServerToClientHandshakePacket();
            packet.setJwt(EncryptionUtils.createHandshakeJwt(serverKeyPair, salt));

            this.awaitingResponse = true;
            this.session.sendPacketImmediately(packet);
            this.session.enableEncryption(secretKey);
        } catch (Exception exception) {
            log.debug("Unable to start encryption handshake for {}", this.session.getSocketAddress(), exception);
            this.loginContext.disconnect(DisconnectFailReason.NOT_AUTHENTICATED,
                    Component.text(this.server.getLanguage().translate("disconnectionScreen.notAuthenticated")));
        }
    }

    @Override
    public PacketSignal handle(ClientToServerHandshakePacket packet) {
        if (!this.awaitingResponse) {
            this.loginContext.disconnect(DisconnectFailReason.NOT_AUTHENTICATED,
                    Component.text(this.server.getLanguage().translate("disconnectionScreen.notAuthenticated")));
            return PacketSignal.HANDLED;
        }

        this.awaitingResponse = false;
        this.session.setPacketHandler(new ResourcePackPacketHandler(this.session, this.server, this.loginContext));

        PlayStatusPacket statusPacket = new PlayStatusPacket();
        statusPacket.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        this.session.sendPacket(statusPacket);
        this.session.sendPacket(this.server.getPackManager().getPacksInfos());

        return PacketSignal.HANDLED;
    }
}
