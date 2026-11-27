package org.cloudburstmc.server.network;

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.bedrock.data.CompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.BatchCompression;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionStrategy;

import java.util.Objects;

/**
 * Compresses outgoing batches of at least the threshold size and sends smaller batches uncompressed.
 */
public class ThresholdCompressionStrategy implements CompressionStrategy {

    private final CompressionStrategy delegate;
    private final int threshold;

    public ThresholdCompressionStrategy(CompressionStrategy delegate, int threshold) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        if (threshold < 0) {
            throw new IllegalArgumentException("Compression threshold must not be negative");
        }
        this.threshold = threshold;
    }

    @Override
    public BatchCompression getCompression(BedrockBatchWrapper wrapper) {
        ByteBuf uncompressed = wrapper.getUncompressed();
        return uncompressed != null && uncompressed.readableBytes() < this.threshold
                ? this.delegate.getCompression(PacketCompressionAlgorithm.NONE)
                : this.delegate.getDefaultCompression();
    }

    @Override
    public BatchCompression getCompression(CompressionAlgorithm algorithm) {
        return this.delegate.getCompression(algorithm);
    }

    @Override
    public BatchCompression getDefaultCompression() {
        return this.delegate.getDefaultCompression();
    }
}
