package org.cloudburstmc.server.player.handler;

import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.network.BedrockInterface;
import org.cloudburstmc.server.network.ProtocolInfo;

public class NetworkSettingsPacketHandler implements BedrockPacketHandler {

    private final BedrockServerSession session;
    private final CloudServer server;
    private final BedrockInterface interfaz;

    public NetworkSettingsPacketHandler(BedrockServerSession session, CloudServer server, BedrockInterface interfaz) {
        this.session = session;
        this.server = server;
        this.interfaz = interfaz;
    }

    @Override
    public PacketSignal handle(RequestNetworkSettingsPacket packet) {
        int protocolVersion = packet.getProtocolVersion();
        BedrockCodec codec = ProtocolInfo.getPacketCodec(protocolVersion);
        if (codec == null) {
            PlayStatusPacket statusPacket = new PlayStatusPacket();
            statusPacket.setStatus(protocolVersion < ProtocolInfo.getDefaultProtocolVersion()
                    ? PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD
                    : PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD);
            this.session.sendPacketImmediately(statusPacket);
            return PacketSignal.HANDLED;
        }

        this.session.setCodec(codec);

        NetworkSettingsPacket networkSettings = new NetworkSettingsPacket();
        networkSettings.setCompressionThreshold(1);
        networkSettings.setCompressionAlgorithm(PacketCompressionAlgorithm.ZLIB);
        this.session.sendPacketImmediately(networkSettings);
        this.session.setCompression(PacketCompressionAlgorithm.ZLIB);
        this.session.setPacketHandler(new LoginPacketHandler(this.session, this.server, this.interfaz));
        return PacketSignal.HANDLED;
    }
}
