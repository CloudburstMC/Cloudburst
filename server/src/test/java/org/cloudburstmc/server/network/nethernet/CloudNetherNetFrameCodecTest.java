package org.cloudburstmc.server.network.nethernet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CloudNetherNetFrameCodecTest {

    @Test
    void wrapsOnlyReadableBytesWithoutConsumingAFrameId() {
        EmbeddedChannel channel = new EmbeddedChannel(new CloudNetherNetFrameCodec());
        ByteBuf input = Unpooled.buffer().writeByte(99).writeByte(0xfe).writeByte(7);
        input.readByte();

        try {
            assertTrue(channel.writeInbound(input));

            BedrockBatchWrapper batch = channel.readInbound();

            try {
                assertNull(batch.getUncompressed());
                assertEquals(2, batch.getCompressed().readableBytes());
                assertEquals(0xfe, batch.getCompressed().readUnsignedByte());
                assertEquals(7, batch.getCompressed().readUnsignedByte());
                assertEquals(1, input.refCnt());
            } finally {
                batch.release();
            }

            assertEquals(0, input.refCnt());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void emitsCompressedBytesWithoutAddingAFrameIdAndTransfersOwnership() {
        EmbeddedChannel channel = new EmbeddedChannel(new CloudNetherNetFrameCodec());
        ByteBuf compressed = Unpooled.buffer().writeByte(42).writeByte(3).writeByte(4);
        compressed.readByte();
        ByteBuf uncompressed = Unpooled.buffer().writeByte(5);

        try {
            assertTrue(channel.writeOutbound(BedrockBatchWrapper.newInstance(compressed, uncompressed)));

            ByteBuf output = channel.readOutbound();

            try {
                assertEquals(2, output.readableBytes());
                assertEquals(3, output.readUnsignedByte());
                assertEquals(4, output.readUnsignedByte());

                assertEquals(0, uncompressed.refCnt());
                assertEquals(1, compressed.refCnt());
            } finally {
                output.release();
            }

            assertEquals(0, compressed.refCnt());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void rejectsEmptyFramesAndUncompressedOutboundBatchesWithoutLeaks() {
        EmbeddedChannel channel = new EmbeddedChannel(new CloudNetherNetFrameCodec());
        ByteBuf empty = Unpooled.buffer(1);
        ByteBuf uncompressed = Unpooled.buffer().writeByte(1);

        try {
            assertThrows(DecoderException.class, () -> channel.writeInbound(empty));
            assertEquals(0, empty.refCnt());

            assertThrows(EncoderException.class,
                    () -> channel.writeOutbound(BedrockBatchWrapper.newInstance(null, uncompressed)));
            assertEquals(0, uncompressed.refCnt());

            assertNull(channel.readInbound());
            assertNull(channel.readOutbound());
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
