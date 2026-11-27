package org.cloudburstmc.server.network.nethernet;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.SystemPropertyUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
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
import org.cloudburstmc.server.network.PacketLimiter;
import org.cloudburstmc.server.network.ThresholdCompressionStrategy;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class CloudNetherNetPeer extends BedrockPeer {

    private static final int DISCONNECT_TIMEOUT_SECONDS = SystemPropertyUtil.getInt("org.cloudburstmc.protocol.bedrock.disconnectTimeout", 10);
    private static final AttributeKey<Boolean> DISCONNECT_PENDING = AttributeKey.valueOf(CloudNetherNetPeer.class, "disconnectPending");

    private static final CompressionStrategy ZLIB = new SimpleCompressionStrategy(new ZlibCompression(Zlib.RAW));
    private static final CompressionStrategy SNAPPY = new SimpleCompressionStrategy(new SnappyCompression());
    private static final CompressionStrategy NONE = new SimpleCompressionStrategy(new NoopCompression());

    private final GameplayPacketQueue gameplayPackets = new GameplayPacketQueue();
    private @Nullable ScheduledFuture<?> disconnectDeadline;
    private boolean gameplay;

    public CloudNetherNetPeer(Channel channel, BedrockSessionFactory sessionFactory) {
        super(channel, sessionFactory);
    }

    @Override
    public void setCompression(PacketCompressionAlgorithm algorithm) {
        this.setCompression(algorithm, 0);
    }

    public void setCompression(PacketCompressionAlgorithm algorithm, int threshold) {
        CompressionStrategy strategy = switch (Objects.requireNonNull(algorithm, "algorithm")) {
            case ZLIB -> ZLIB;
            case SNAPPY -> SNAPPY;
            case NONE -> NONE;
        };

        super.setCompression(new ThresholdCompressionStrategy(strategy, threshold));
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
    public GameplayPacketQueue startGameplay(BedrockServerSession session, BedrockPacketHandler disconnectHandler, PacketLimiter limiter) {
        if (session.getPeer() != this || session.isSubClient()) {
            throw new IllegalStateException("Only the primary session can start gameplay");
        }

        this.gameplayPackets.limit(limiter);

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
     * Sends the first disconnect reason and gives the client time to close the connection.
     */
    public void disconnectSession(BedrockServerSession session, DisconnectFailReason reason, Component message, Locale locale) {
        if (session.getPeer() != this || session.isSubClient()) {
            throw new IllegalArgumentException("Only this peer's primary session controls the connection");
        }

        Channel channel = this.getChannel();
        if (!channel.isOpen() || channel.attr(DISCONNECT_PENDING).setIfAbsent(true) != null) {
            return;
        }

        EventLoop loop = channel.eventLoop();
        if (loop.inEventLoop()) {
            this.beginDisconnect(session, reason, message, locale);
            return;
        }

        try {
            loop.execute(() -> this.beginDisconnect(session, reason, message, locale));
        } catch (RejectedExecutionException rejected) {
            channel.close();
        }
    }

    public static boolean hasPendingDisconnect(Channel channel) {
        return channel.isOpen() && channel.hasAttr(DISCONNECT_PENDING) && Boolean.TRUE.equals(channel.attr(DISCONNECT_PENDING).get());
    }

    @SuppressWarnings("resource") // The event loop belongs to the channel.
    private void beginDisconnect(BedrockServerSession session, DisconnectFailReason reason, Component message, Locale locale) {
        if (!session.isConnected()) {
            return;
        }

        Channel channel = this.getChannel();
        if (channel.pipeline().get(CloudLoginTimeoutHandler.class) != null) {
            channel.pipeline().remove(CloudLoginTimeoutHandler.class);
        }
        this.blackholeInboundPackets();

        BedrockComponent kickMessage = new BedrockComponent(GlobalTranslator.render(message, locale));
        DisconnectPacket packet = new DisconnectPacket();
        packet.setReason(reason);
        packet.setKickMessage(kickMessage);
        this.disconnectDeadline = channel.eventLoop().schedule(() -> {
            if (session.isConnected()) {
                session.close(kickMessage);
            }
        }, DISCONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        session.sendPacketImmediately(packet);
    }

    @Override
    protected void onClose() {
        if (!this.getChannel().isOpen()) {
            if (this.disconnectDeadline != null) {
                this.disconnectDeadline.cancel(false);
                this.disconnectDeadline = null;
            }
            this.gameplayPackets.close();
        }
        super.onClose();
    }
}
