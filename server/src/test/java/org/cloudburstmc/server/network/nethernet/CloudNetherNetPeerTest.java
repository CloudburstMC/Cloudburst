package org.cloudburstmc.server.network.nethernet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.adventure.BedrockComponent;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v622.Bedrock_v622;
import org.cloudburstmc.protocol.bedrock.codec.v649.Bedrock_v649;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.*;
import org.cloudburstmc.protocol.bedrock.netty.codec.encryption.BedrockEncryptionDecoder;
import org.cloudburstmc.protocol.bedrock.netty.codec.encryption.BedrockEncryptionEncoder;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec_v3;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.*;

class CloudNetherNetPeerTest {

    private static final byte[] PAYLOAD = "A NetherNet batch with repeated repeated repeated data".getBytes(StandardCharsets.US_ASCII);

    @Test
    void sendsOneDisconnectPacketWithTheTypedReasonAndComponentMessage() {
        EmbeddedChannel channel = newChannel();
        channel.freezeTime();
        CloudNetherNetPeer peer = new CloudNetherNetPeer(channel, BedrockServerSession::new);
        RecordingSession session = new RecordingSession(peer);
        Component message = Component.text("Denied");

        try {
            peer.disconnectSession(session, DisconnectFailReason.NOT_ALLOWED, message);
            channel.runPendingTasks();

            assertEquals(1, session.packets.size());

            DisconnectPacket packet = assertInstanceOf(DisconnectPacket.class, session.packets.getFirst());

            assertEquals(DisconnectFailReason.NOT_ALLOWED, packet.getReason());
            assertEquals(message, assertInstanceOf(BedrockComponent.class,
                    packet.getKickMessage(CharSequence.class)).asComponent());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void givesTheDisconnectPacketAGracePeriodBeforeClosingWithoutSendingAnotherPacket() {
        EmbeddedChannel channel = newChannel();
        channel.freezeTime();
        CloudNetherNetPeer peer = new CloudNetherNetPeer(channel, BedrockServerSession::new);
        RecordingSession session = new RecordingSession(peer);
        Component message = Component.text("Denied");

        try {
            peer.disconnectSession(session, DisconnectFailReason.KICKED, message);
            channel.runPendingTasks();

            assertNull(session.closeReason);

            channel.advanceTimeBy(Integer.getInteger("org.cloudburstmc.protocol.bedrock.disconnectTimeout", 10), TimeUnit.SECONDS);
            channel.runScheduledPendingTasks();

            assertEquals(message, assertInstanceOf(BedrockComponent.class, session.closeReason).asComponent());
            assertEquals(1, session.packets.size());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void rejectsDisconnectingASessionOwnedByAnotherPeer() {
        EmbeddedChannel channel = newChannel();
        EmbeddedChannel otherChannel = newChannel();
        CloudNetherNetPeer peer = new CloudNetherNetPeer(channel, BedrockServerSession::new);
        RecordingSession session = new RecordingSession(new CloudNetherNetPeer(otherChannel, BedrockServerSession::new));

        try {
            assertThrows(IllegalArgumentException.class,
                    () -> peer.disconnectSession(session, DisconnectFailReason.KICKED, Component.empty()));
            assertTrue(session.packets.isEmpty());
        } finally {
            channel.finishAndReleaseAll();
            otherChannel.finishAndReleaseAll();
        }
    }

    @Test
    void negotiatesAllAlgorithmsWithoutTransportOptionsAndRoundTripsBothPrefixModes() throws Exception {
        for (BedrockCodec codec : new BedrockCodec[]{Bedrock_v622.CODEC, Bedrock_v649.CODEC}) {
            EmbeddedChannel channel = newChannel();
            CloudNetherNetPeer peer = new CloudNetherNetPeer(channel, BedrockServerSession::new);
            peer.setCodec(codec);

            try {
                for (PacketCompressionAlgorithm algorithm : PacketCompressionAlgorithm.values()) {
                    peer.setCompression(algorithm);

                    assertEquals(algorithm, peer.getCompressionStrategy().getDefaultCompression().getAlgorithm());
                    Class<? extends BatchCompression> expectedCompression = switch (algorithm) {
                        case ZLIB -> ZlibCompression.class;
                        case SNAPPY -> SnappyCompression.class;
                        case NONE -> NoopCompression.class;
                    };

                    assertEquals(expectedCompression, peer.getCompressionStrategy().getDefaultCompression().getClass());

                    assertTrue(channel.writeOutbound(
                            BedrockBatchWrapper.newInstance(null, Unpooled.wrappedBuffer(PAYLOAD))));

                    ByteBuf wire = channel.readOutbound();

                    try {
                        int prefix = codec.getProtocolVersion() >= 649 ? 1 : 0;

                        if (prefix != 0) {
                            int expected = switch (algorithm) {
                                case ZLIB -> 0;
                                case SNAPPY -> 1;
                                case NONE -> 255;
                            };

                            assertEquals(expected, wire.getUnsignedByte(wire.readerIndex()));
                        }

                        if (algorithm == PacketCompressionAlgorithm.ZLIB) {
                            assertRawDeflate(wire, prefix);
                        } else if (algorithm == PacketCompressionAlgorithm.NONE) {
                            assertEquals(PAYLOAD.length + prefix, wire.readableBytes());
                            assertArrayEquals(PAYLOAD, ByteBufUtil.getBytes(wire, wire.readerIndex() + prefix,
                                    wire.readableBytes() - prefix));
                        }

                        assertTrue(channel.writeInbound(wire.retainedDuplicate()));

                        BedrockBatchWrapper decoded = channel.readInbound();

                        try {
                            assertEquals(algorithm, decoded.getAlgorithm());
                            assertArrayEquals(PAYLOAD, ByteBufUtil.getBytes(decoded.getUncompressed()));
                        } finally {
                            decoded.release();
                        }
                    } finally {
                        wire.release();
                    }
                }
            } finally {
                channel.finishAndReleaseAll();
            }
        }
    }

    @Test
    void rejectsGameEncryptionAndNullCompressionWithoutChangingThePipeline() {
        EmbeddedChannel channel = newChannel();
        CloudNetherNetPeer peer = new CloudNetherNetPeer(channel, BedrockServerSession::new);

        try {
            CompressionCodec initial = channel.pipeline().get(CompressionCodec.class);

            assertThrows(NullPointerException.class,
                    () -> peer.setCompression((PacketCompressionAlgorithm) null));
            assertSame(initial, channel.pipeline().get(CompressionCodec.class));

            assertThrows(UnsupportedOperationException.class,
                    () -> peer.enableEncryption(new SecretKeySpec(new byte[32], "AES")));
            assertNull(channel.pipeline().get(BedrockEncryptionEncoder.class));
            assertNull(channel.pipeline().get(BedrockEncryptionDecoder.class));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    private static EmbeddedChannel newChannel() {
        EmbeddedChannel channel = new EmbeddedChannel();

        channel.pipeline()
                .addLast(CloudNetherNetFrameCodec.NAME, new CloudNetherNetFrameCodec())
                .addLast(CompressionCodec.NAME,
                        new CompressionCodec(new SimpleCompressionStrategy(new NoopCompression()), false))
                .addLast(BedrockPacketCodec.NAME, new BedrockPacketCodec_v3());

        return channel;
    }

    private static class RecordingSession extends BedrockServerSession {

        private final List<BedrockPacket> packets = new ArrayList<>();
        private CharSequence closeReason;

        private RecordingSession(BedrockPeer peer) {
            super(peer, 0);
        }

        @Override
        public void sendPacketImmediately(@NonNull BedrockPacket packet) {
            this.packets.add(packet);
        }

        @Override
        public void close(CharSequence reason) {
            this.closeReason = reason;
        }
    }

    private static void assertRawDeflate(ByteBuf wire, int prefix) throws Exception {
        try (Inflater inflater = new Inflater(true)) {
            inflater.setInput(ByteBufUtil.getBytes(wire, wire.readerIndex() + prefix, wire.readableBytes() - prefix));
            byte[] result = new byte[PAYLOAD.length + 1];
            int size = inflater.inflate(result);

            assertTrue(inflater.finished());
            assertEquals(PAYLOAD.length, size);
            assertArrayEquals(PAYLOAD, java.util.Arrays.copyOf(result, size));
        }
    }
}
