package org.cloudburstmc.server.network.nethernet;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.NoopCompression;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.SimpleCompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec_v3;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer;

import java.util.function.Consumer;

@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class CloudNetherNetServerInitializer extends BedrockServerInitializer {

    private static final CloudNetherNetFrameCodec FRAME_CODEC = new CloudNetherNetFrameCodec();

    @NonNull
    private final Consumer<BedrockServerSession> sessionInitializer;

    @Override
    protected void preInitChannel(Channel channel) {
        channel.attr(PacketDirection.ATTRIBUTE).set(PacketDirection.CLIENT_BOUND);
        channel.pipeline()
                .addLast(CloudNetherNetFrameCodec.NAME, FRAME_CODEC)
                .addLast(CompressionCodec.NAME, new CompressionCodec(new SimpleCompressionStrategy(new NoopCompression()), false));
    }

    @Override
    protected void initPacketCodec(Channel channel) {
        channel.pipeline().addLast(BedrockPacketCodec.NAME, new BedrockPacketCodec_v3());
    }

    @Override
    protected BedrockPeer createPeer(Channel channel) {
        return new CloudNetherNetPeer(channel, this::createSession);
    }

    @Override
    protected void postInitChannel(Channel channel) {
        channel.pipeline().addLast(CloudNetherNetExceptionHandler.NAME, new CloudNetherNetExceptionHandler());
    }

    @Override
    protected void initSession(BedrockServerSession session) {
        this.sessionInitializer.accept(session);
    }
}
