package org.cloudburstmc.server.network.nethernet;

import io.netty.channel.Channel;
import io.netty.util.internal.SystemPropertyUtil;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.adventure.BedrockComponent;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.BedrockSessionFactory;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.*;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.common.util.Zlib;

import javax.crypto.SecretKey;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class CloudNetherNetPeer extends BedrockPeer {

    private static final int DISCONNECT_TIMEOUT_SECONDS = SystemPropertyUtil.getInt("org.cloudburstmc.protocol.bedrock.disconnectTimeout", 10);

    private static final CompressionStrategy ZLIB = new SimpleCompressionStrategy(new ZlibCompression(Zlib.RAW));
    private static final CompressionStrategy SNAPPY = new SimpleCompressionStrategy(new SnappyCompression());
    private static final CompressionStrategy NONE = new SimpleCompressionStrategy(new NoopCompression());

    public CloudNetherNetPeer(Channel channel, BedrockSessionFactory sessionFactory) {
        super(channel, sessionFactory);
    }

    /**
     * Sends a typed rejection, stops further inbound packets and allows time for delivery before closing.
     */
    @SuppressWarnings("resource")
    public void disconnectSession(BedrockServerSession session, DisconnectFailReason reason, Component message) {
        if (session.getPeer() != this) {
            throw new IllegalArgumentException("Session belongs to a different peer");
        }

        this.getChannel().eventLoop().execute(() -> {
            if (!session.isConnected()) {
                return;
            }

            BedrockComponent kickMessage = new BedrockComponent(message);
            DisconnectPacket packet = new DisconnectPacket();
            packet.setReason(reason);
            packet.setKickMessage(kickMessage);
            session.sendPacketImmediately(packet);
            if (!session.isSubClient()) {
                this.blackholeInboundPackets();
            }

            this.getChannel().eventLoop().schedule(() -> {
                if (session.isConnected()) {
                    session.close(kickMessage);
                }
            }, DISCONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        });
    }

    @Override
    public void setCompression(PacketCompressionAlgorithm algorithm) {
        CompressionStrategy strategy = switch (Objects.requireNonNull(algorithm, "algorithm")) {
            case ZLIB -> ZLIB;
            case SNAPPY -> SNAPPY;
            case NONE -> NONE;
        };

        super.setCompression(strategy);
    }

    @Override
    public void enableEncryption(@NonNull SecretKey secretKey) {
        throw new UnsupportedOperationException("NetherNet uses DTLS. Bedrock game encryption must not be enabled");
    }
}
