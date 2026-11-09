package org.cloudburstmc.server.network.nethernet;

import io.netty.channel.Channel;
import io.netty.util.internal.SystemPropertyUtil;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChildChannel;
import org.cloudburstmc.netty.util.nethernet.PlayerInfo;
import org.cloudburstmc.netty.util.nethernet.TransportIdentityBinding;
import org.cloudburstmc.protocol.adventure.BedrockComponent;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.BedrockSessionFactory;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.*;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.bedrock.packet.SubClientLoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.UnknownPacket;
import org.cloudburstmc.protocol.common.util.Zlib;
import org.cloudburstmc.server.network.GameplayPacketQueue;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class CloudNetherNetPeer extends BedrockPeer {

    private static final int DISCONNECT_TIMEOUT_SECONDS = SystemPropertyUtil.getInt("org.cloudburstmc.protocol.bedrock.disconnectTimeout", 10);

    private static final CompressionStrategy ZLIB = new SimpleCompressionStrategy(new ZlibCompression(Zlib.RAW));
    private static final CompressionStrategy SNAPPY = new SimpleCompressionStrategy(new SnappyCompression());
    private static final CompressionStrategy NONE = new SimpleCompressionStrategy(new NoopCompression());

    private final GameplayPacketQueue gameplayPackets = new GameplayPacketQueue();
    private boolean gameplay;

    public CloudNetherNetPeer(Channel channel, BedrockSessionFactory sessionFactory) {
        super(channel, sessionFactory);
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

    /**
     * Checks the transport key for every login. Only Xbox-authenticated built-in signaling attests an XUID.
     *
     * @return the rejection reason, or null if the identities agree
     */
    public @Nullable String loginIdentityMismatch(PublicKey loginKey, @Nullable String loginXuid, boolean authenticatedTransport) {
        String mismatch = TransportIdentityBinding.mismatch(this.getChannel(), loginKey);
        if (mismatch != null || !authenticatedTransport) {
            return mismatch;
        }

        PlayerInfo player = this.getChannel().attr(NetherNetChildChannel.PLAYER_INFO).get();
        return player == null || loginXuid == null || loginXuid.isBlank() || !loginXuid.equals(player.xuid()) ? "the transport and login XUIDs do not match" : null;
    }

    /**
     * Returns the gameplay queue and schedules the handoff from login on the connection loop.
     */
    @SuppressWarnings("resource") // The shared event loop is owned by the network.
    public GameplayPacketQueue startGameplay(BedrockServerSession session, BedrockPacketHandler disconnectHandler) {
        if (session.getPeer() != this || session.isSubClient()) {
            throw new IllegalStateException("Only the primary session can start gameplay");
        }

        this.getChannel().eventLoop().execute(() -> {
            if (!this.getChannel().isActive() || !session.isConnected()) {
                disconnectHandler.onDisconnect("Connection closed before gameplay");
                return;
            }

            session.setPacketHandler(disconnectHandler);
            this.gameplay = true;
        });
        return this.gameplayPackets;
    }

    @Override
    protected void onBedrockPacket(BedrockPacketWrapper wrapper) {
        if (wrapper.getSenderSubClientId() != 0 || wrapper.getTargetSubClientId() != 0
                || wrapper.getPacket() instanceof SubClientLoginPacket || wrapper.getPacket() instanceof UnknownPacket) {
            return;
        }

        if (!this.gameplay) {
            super.onBedrockPacket(wrapper);
        } else if (this.getChannel().isActive() && !this.gameplayPackets.offer(wrapper.getPacket(), wrapper.getPacketBuffer().readableBytes())) {
            this.gameplayPackets.close();
            this.getChannel().close();
        }
    }

    /**
     * Sends a rejection before closing. Rejecting the primary session also stops further input.
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
    protected void onClose() {
        if (!this.getChannel().isOpen()) {
            this.gameplayPackets.close();
        }
        super.onClose();
    }
}
