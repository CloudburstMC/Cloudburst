package org.cloudburstmc.server.network.nethernet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;

import java.util.List;

/**
 * Encodes and decodes complete batch payloads without a frame header.
 */
@ChannelHandler.Sharable
public class CloudNetherNetFrameCodec extends MessageToMessageCodec<ByteBuf, BedrockBatchWrapper> {

    public static final String NAME = "cloud-nethernet-frame-codec";

    @Override
    protected void encode(ChannelHandlerContext context, BedrockBatchWrapper batch, List<Object> output) {
        ByteBuf compressed = batch.getCompressed();
        if (compressed == null) {
            throw new IllegalStateException("Bedrock batch was not compressed");
        }

        output.add(compressed.retainedSlice());
    }

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf message, List<Object> output) {
        if (!message.isReadable()) {
            throw new DecoderException("Empty NetherNet frame");
        }

        output.add(BedrockBatchWrapper.newInstance(message.retainedSlice(), null));
    }
}
