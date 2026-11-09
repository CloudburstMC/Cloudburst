package org.cloudburstmc.server.network.nethernet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.protocol.adventure.AdventureTextConverter;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.v649.Bedrock_v649;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.netty.codec.FrameIdCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.batch.BedrockBatchDecoder;
import org.cloudburstmc.protocol.bedrock.netty.codec.batch.BedrockBatchEncoder;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec_v3;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.SubClientLoginPacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.cloudburstmc.server.network.GameplayPacketQueue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CloudNetherNetServerInitializerTest {

    @Test
    public void installsHeaderlessStandardBatchPipelineBeforeInvokingSessionSetup() {
        AtomicReference<BedrockServerSession> session = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();

        EmbeddedChannel channel = new EmbeddedChannel(new CloudNetherNetServerInitializer(created -> {
            calls.incrementAndGet();
            session.set(created);

            assertInstanceOf(CloudNetherNetPeer.class, created.getPeer());
            assertNotNull(created.getPeer().getCodecHelper());
            assertInstanceOf(BedrockPacketCodec_v3.class,
                    created.getPeer().getChannel().pipeline().get(BedrockPacketCodec_v3.class));
        }));

        try {
            channel.checkException();

            assertEquals(1, calls.get());
            assertSame(channel.pipeline().get(BedrockPeer.class), session.get().getPeer());
            assertEquals(PacketDirection.CLIENT_BOUND, channel.attr(PacketDirection.ATTRIBUTE).get());

            assertNotNull(channel.pipeline().get(CloudNetherNetFrameCodec.class));
            assertNull(channel.pipeline().get(FrameIdCodec.class));
            assertEquals(BedrockBatchEncoder.class, channel.pipeline().get(BedrockBatchEncoder.class).getClass());
            assertNotNull(channel.pipeline().get(BedrockBatchDecoder.class));

            assertEquals(PacketCompressionAlgorithm.NONE,
                    session.get().getPeer().getCompressionStrategy().getDefaultCompression().getAlgorithm());

            List<String> names = channel.pipeline().names();

            assertTrue(names.indexOf(CloudNetherNetFrameCodec.NAME) < names.indexOf(CompressionCodec.NAME));
            assertTrue(names.indexOf(CompressionCodec.NAME) < names.indexOf(BedrockBatchDecoder.NAME));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    public void sessionCallbackCanConfigureTextConversionAfterSelectingTheCodec() {
        AtomicReference<BedrockServerSession> session = new AtomicReference<>();
        AdventureTextConverter converter = new AdventureTextConverter();

        EmbeddedChannel channel = new EmbeddedChannel(new CloudNetherNetServerInitializer(created -> {
            BedrockCodecHelper initialHelper = created.getPeer().getCodecHelper();
            assertNotNull(initialHelper);

            created.setCodec(Bedrock_v649.CODEC);

            assertNotSame(initialHelper, created.getPeer().getCodecHelper());

            created.getPeer().getCodecHelper().setTextConverter(converter);
            session.set(created);
        }));

        try {
            channel.checkException();

            assertNotNull(session.get());
            assertSame(converter, session.get().getPeer().getCodecHelper().getTextConverter());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    public void batchesTwoPacketsIntoOneUnprefixedFrameAndDispatchesBothOnReceive() {
        AtomicReference<BedrockServerSession> session = new AtomicReference<>();
        List<Integer> receivedVersions = new ArrayList<>();

        EmbeddedChannel channel = new EmbeddedChannel(new CloudNetherNetServerInitializer(created -> {
            created.setCodec(Bedrock_v649.CODEC);
            created.setPacketHandler(new BedrockPacketHandler() {
                @Override
                public PacketSignal handle(RequestNetworkSettingsPacket packet) {
                    receivedVersions.add(packet.getProtocolVersion());
                    return PacketSignal.HANDLED;
                }
            });

            session.set(created);
        }));

        try {
            channel.checkException();

            RequestNetworkSettingsPacket first = new RequestNetworkSettingsPacket();
            first.setProtocolVersion(649);

            RequestNetworkSettingsPacket second = new RequestNetworkSettingsPacket();
            second.setProtocolVersion(650);

            session.get().getPeer().sendPacketsImmediately(0, 0, first, second);

            ByteBuf frame = channel.readOutbound();
            assertNotNull(frame);

            try {
                assertNull(channel.readOutbound());
                assertFalse(channel.writeInbound(frame.retainedDuplicate()));
                assertEquals(List.of(649, 650), receivedVersions);
            } finally {
                frame.release();
            }
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void keepsSecondarySessionsAndUnknownPacketsOutOfGameplay() {
        AtomicInteger sessions = new AtomicInteger();
        AtomicReference<GameplayPacketQueue> gameplay = new AtomicReference<>();
        EmbeddedChannel channel = new EmbeddedChannel(new CloudNetherNetServerInitializer(session -> {
            sessions.incrementAndGet();
            session.setCodec(Bedrock_v649.CODEC);
            CloudNetherNetPeer peer = assertInstanceOf(CloudNetherNetPeer.class, session.getPeer());
            gameplay.set(peer.startGameplay(session, new BedrockPacketHandler() {
            }));
        }));

        try {
            channel.runPendingTasks();
            channel.writeInbound(BedrockPacketWrapper.create(0, 1, 1, new SubClientLoginPacket(), null));
            channel.writeInbound(BedrockPacketWrapper.create(0, 1, 0, new RequestNetworkSettingsPacket(), null));

            ByteBuf unknown = Unpooled.buffer();
            VarInts.writeUnsignedInt(unknown, 3);
            VarInts.writeUnsignedInt(unknown, 1023);
            unknown.writeByte(42);
            channel.writeInbound(unknown);
            assertEquals(0, unknown.refCnt());

            RequestNetworkSettingsPacket request = new RequestNetworkSettingsPacket();
            request.setProtocolVersion(649);
            channel.pipeline().get(BedrockPeer.class).sendPacketImmediately(0, 0, request);
            channel.writeInbound(channel.<ByteBuf>readOutbound());

            List<Integer> versions = new ArrayList<>();
            gameplay.get().drain(packet ->
                    versions.add(assertInstanceOf(RequestNetworkSettingsPacket.class, packet).getProtocolVersion()));
            assertEquals(List.of(649), versions);
            assertEquals(1, sessions.get());
            assertTrue(channel.isActive());
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
