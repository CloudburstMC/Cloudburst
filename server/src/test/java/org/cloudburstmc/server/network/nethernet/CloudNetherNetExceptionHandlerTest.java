package org.cloudburstmc.server.network.nethernet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CloudNetherNetExceptionHandlerTest {

    @Test
    void closesOnAnUpstreamExceptionWithoutLeavingAnUnhandledException() {
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext context, Object message) {
                throw new IllegalStateException("Invalid incoming message");
            }
        }, new CloudNetherNetExceptionHandler());

        try {
            assertDoesNotThrow(() -> channel.writeInbound("malformed message"));
            channel.runPendingTasks();

            assertDoesNotThrow(channel::checkException);
            assertFalse(channel.isOpen());
            assertNull(channel.readInbound());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void leavesHealthyTrafficAndLifecycleEventsUnchanged() {
        EmbeddedChannel channel = new EmbeddedChannel(new CloudNetherNetExceptionHandler());
        ByteBuf input = Unpooled.buffer().writeByte(7);

        try {
            assertTrue(channel.isActive());
            assertTrue(channel.writeInbound(input));

            ByteBuf output = channel.readInbound();

            try {
                assertSame(input, output);
                assertEquals(7, output.readUnsignedByte());
                assertTrue(channel.isActive());
            } finally {
                output.release();
            }

            assertEquals(0, input.refCnt());
            assertDoesNotThrow(channel::checkException);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void initializerAppendsTheHandlerLastAndClosesOnMalformedFrames() {
        EmbeddedChannel channel = new EmbeddedChannel(new CloudNetherNetServerInitializer(session -> { }));
        ByteBuf empty = Unpooled.buffer(1);

        try {
            assertNotNull(channel.pipeline().get(BedrockPeer.class));
            assertInstanceOf(CloudNetherNetExceptionHandler.class, channel.pipeline().last());
            assertSame(channel.pipeline().last(), channel.pipeline().get(CloudNetherNetExceptionHandler.NAME));

            assertDoesNotThrow(() -> channel.writeInbound(empty));
            channel.runPendingTasks();

            assertDoesNotThrow(channel::checkException);
            assertFalse(channel.isOpen());
            assertEquals(0, empty.refCnt());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void catchesSessionSetupFailuresAfterThePipelineIsInstalled() {
        EmbeddedChannel channel = new EmbeddedChannel(new CloudNetherNetServerInitializer(session -> {
            throw new IllegalStateException("Session setup failed");
        }));

        try {
            channel.runPendingTasks();

            assertDoesNotThrow(channel::checkException);
            assertFalse(channel.isOpen());
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
