package org.cloudburstmc.server.player.handler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.adventure.AdventureTextConverter;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.network.ProtocolInfo;
import org.cloudburstmc.server.network.nethernet.CloudNetherNetPeer;

@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class NetworkSettingsPacketHandler implements BedrockPacketHandler {

    private final BedrockServerSession session;
    private final CloudServer server;
    private final boolean authenticatedTransport;

    @Override
    public PacketSignal handle(RequestNetworkSettingsPacket packet) {
        int protocolVersion = packet.getProtocolVersion();
        BedrockCodec codec = ProtocolInfo.getPacketCodec(protocolVersion);
        if (codec == null) {
            this.server.getNetwork().reportExternalRejection(this.session.getPeer().getChannel(), "unsupported_version");
            PlayStatusPacket statusPacket = new PlayStatusPacket();
            statusPacket.setStatus(protocolVersion < ProtocolInfo.getDefaultProtocolVersion()
                    ? PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD
                    : PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD);
            this.session.sendPacketImmediately(statusPacket);
            return PacketSignal.HANDLED;
        }

        this.session.setCodec(codec);
        this.session.getPeer().getCodecHelper().setTextConverter(new AdventureTextConverter());

        int threshold = this.server.getConfig().getNetworkCompressionThreshold();
        PacketCompressionAlgorithm algorithm = switch (this.server.getConfig().getNetwork().getCompressionAlgorithm()) {
            case ZLIB -> PacketCompressionAlgorithm.ZLIB;
            case SNAPPY -> PacketCompressionAlgorithm.SNAPPY;
        };

        NetworkSettingsPacket networkSettings = new NetworkSettingsPacket();
        networkSettings.setCompressionThreshold(threshold);
        networkSettings.setCompressionAlgorithm(algorithm);
        this.session.sendPacketImmediately(networkSettings);
        ((CloudNetherNetPeer) this.session.getPeer()).setCompression(algorithm, threshold);
        this.session.setPacketHandler(new LoginPacketHandler(this.session, this.server, this.authenticatedTransport));
        return PacketSignal.HANDLED;
    }
}
