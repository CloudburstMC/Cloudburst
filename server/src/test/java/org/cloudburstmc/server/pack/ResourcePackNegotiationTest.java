package org.cloudburstmc.server.pack;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.protocol.adventure.AdventureTextConverter;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.server.network.ProtocolInfo;
import org.cloudburstmc.server.network.nethernet.CloudNetherNetServerInitializer;
import org.cloudburstmc.server.player.handler.ResourcePackPacketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.cloudburstmc.protocol.bedrock.packet.ResourcePackClientResponsePacket.Status.*;
import static org.junit.jupiter.api.Assertions.*;

class ResourcePackNegotiationTest {

    @TempDir
    private Path directory;

    @Test
    void transfersOnlyRequestedChunksAndCompletesOnceAfterTheStack() throws IOException {
        try (Connection connection = new Connection(this.directory)) {
            connection.respond(SEND_PACKS, connection.first + "_1.0.0", connection.second + "_1.0.0");
            connection.respond(SEND_PACKS, connection.first + "_1.0.0");
            connection.flush();
            assertEquals(List.of(connection.first), connection.offers);

            connection.channel.unsafe().outboundBuffer().setUserDefinedWritability(1, false);
            connection.requestFirstChunk(connection.first);
            connection.requestFirstChunk(connection.first);
            assertTrue(connection.reads.isEmpty());
            connection.channel.unsafe().outboundBuffer().setUserDefinedWritability(1, true);
            connection.channel.runPendingTasks();
            assertEquals(1, connection.reads.size());

            connection.readNext();
            assertEquals(List.of(connection.first, connection.second), connection.offers);
            assertEquals(List.of(connection.first), connection.chunks);
            connection.requestFirstChunk(connection.second);
            connection.readNext();
            assertEquals(List.of(connection.first, connection.second), connection.chunks);

            connection.respond(HAVE_ALL_PACKS);
            connection.respond(HAVE_ALL_PACKS);
            connection.respond(COMPLETED);
            connection.respond(COMPLETED);
            connection.flush();

            assertEquals(1, connection.stacks.get());
            assertEquals(1, connection.completions.get());
            assertNull(connection.rejection.get());
        }
    }

    @Test
    void rejectsPrematureCompletionWithoutSchedulingLogin() throws IOException {
        try (Connection connection = new Connection(this.directory)) {
            connection.respond(COMPLETED);
            connection.flush();

            assertEquals(DisconnectFailReason.RESOURCE_PACK_PROBLEM, connection.rejection.get());
            assertEquals(0, connection.completions.get());
            assertTrue(connection.reads.isEmpty());
        }
    }

    @Test
    void rejectsAChunkForAPackThatHasNotBeenAnnounced() throws IOException {
        try (Connection connection = new Connection(this.directory)) {
            connection.respond(SEND_PACKS, connection.first + "_1.0.0", connection.second + "_1.0.0");
            connection.requestFirstChunk(connection.second);
            connection.flush();

            assertEquals(DisconnectFailReason.RESOURCE_PACK_PROBLEM, connection.rejection.get());
            assertTrue(connection.reads.isEmpty());
        }
    }

    private static class Connection implements AutoCloseable {
        private final UUID first = UUID.randomUUID();
        private final UUID second = UUID.randomUUID();
        private final CloudPackManager packs = new CloudPackManager();
        private final ArrayDeque<Runnable> reads = new ArrayDeque<>();
        private final List<UUID> offers = new ArrayList<>();
        private final List<UUID> chunks = new ArrayList<>();
        private final AtomicInteger stacks = new AtomicInteger();
        private final AtomicInteger completions = new AtomicInteger();
        private final AtomicReference<DisconnectFailReason> rejection = new AtomicReference<>();
        private final EmbeddedChannel channel;

        private Connection(Path directory) throws IOException {
            this.packs.loadPack(CloudPackTestFiles.directory(directory.resolve("first"), this.first, ""));
            this.packs.loadPack(CloudPackTestFiles.directory(directory.resolve("second"), this.second, ""));
            this.packs.close();
            this.channel = new EmbeddedChannel(new CloudNetherNetServerInitializer(session -> {
                session.setCodec(ProtocolInfo.getDefaultPacketCodec());
                session.getPeer().getCodecHelper().setTextConverter(new AdventureTextConverter());
                ResourcePackPacketHandler handler = new ResourcePackPacketHandler(session, this.packs, false,
                        this.reads::offer, this.completions::incrementAndGet, _ -> {});
                session.setPacketHandler(handler);
                handler.offer();
            }), new ChannelOutboundHandlerAdapter() {
                @Override
                public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
                    if (message instanceof BedrockPacketWrapper wrapper) {
                        switch (wrapper.getPacket()) {
                            case ResourcePackDataInfoPacket info -> Connection.this.offers.add(info.getPackId());
                            case ResourcePackChunkDataPacket chunk -> Connection.this.chunks.add(chunk.getPackId());
                            case ResourcePackStackPacket ignored -> Connection.this.stacks.incrementAndGet();
                            case DisconnectPacket packet -> Connection.this.rejection.set(packet.getReason());
                            default -> {}
                        }
                    }
                    context.write(message, promise);
                }
            });
            this.channel.freezeTime();
        }

        private void respond(ResourcePackClientResponsePacket.Status status, String... packs) {
            ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
            response.setStatus(status);
            response.getPackIds().addAll(List.of(packs));
            this.channel.writeInbound(BedrockPacketWrapper.create(0, 0, 0, response, null));
        }

        private void requestFirstChunk(UUID pack) {
            ResourcePackChunkRequestPacket request = new ResourcePackChunkRequestPacket();
            request.setPackId(pack);
            request.setPackVersion("1.0.0");
            request.setChunkIndex(0);
            this.channel.writeInbound(BedrockPacketWrapper.create(0, 0, 0, request, null));
        }

        private void readNext() {
            this.reads.removeFirst().run();
            this.flush();
        }

        private void flush() {
            this.channel.runPendingTasks();
            this.channel.advanceTimeBy(20, TimeUnit.MILLISECONDS);
            this.channel.runScheduledPendingTasks();
            this.channel.checkException();
        }

        @Override
        public void close() {
            try {
                this.channel.finishAndReleaseAll();
            } finally {
                this.packs.shutdown();
            }
        }
    }
}
