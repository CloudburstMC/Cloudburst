package org.cloudburstmc.server.player.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.bedrock.data.ResourcePackType;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.server.network.nethernet.CloudNetherNetPeer;
import org.cloudburstmc.server.pack.CloudPackManager;
import org.cloudburstmc.server.pack.CloudResourcePack;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Negotiates resource packs on the connection loop. File reads run off-loop, one chunk at a time per connection.
 */
@Log4j2
public class ResourcePackPacketHandler implements BedrockPacketHandler {

    private static final int CHUNK_SIZE = 8 * 1024;
    private static final int MAX_PENDING_CHUNKS = 64;

    private final BedrockServerSession session;
    private final CloudNetherNetPeer peer;
    private final CloudPackManager packs;
    private final Predicate<Runnable> submitRead;
    private final Runnable onComplete;
    private final Consumer<Boolean> onCacheStatus;
    private final Channel channel;
    private final boolean required;
    private final ArrayDeque<CloudResourcePack> requestedPacks = new ArrayDeque<>();
    private final ArrayDeque<Integer> pendingChunks = new ArrayDeque<>();
    private final BitSet requestedChunks = new BitSet();
    private final ChannelInboundHandlerAdapter writableListener = new ChannelInboundHandlerAdapter() {
        @Override
        public void channelWritabilityChanged(ChannelHandlerContext context) {
            ResourcePackPacketHandler.this.transferNextChunk();
            context.fireChannelWritabilityChanged();
        }
    };

    private Phase phase = Phase.OFFERED;
    private CloudResourcePack currentPack;
    private int chunkCount;
    private int chunksWritten;
    private boolean chunkInFlight;

    public ResourcePackPacketHandler(BedrockServerSession session, CloudPackManager packs, boolean required,
                                     Predicate<Runnable> submitRead, Runnable onComplete, Consumer<Boolean> onCacheStatus) {
        this.session = session;
        this.peer = (CloudNetherNetPeer) session.getPeer();
        this.channel = this.peer.getChannel();
        this.packs = packs;
        this.required = required;
        this.submitRead = submitRead;
        this.onComplete = onComplete;
        this.onCacheStatus = onCacheStatus;
    }

    public void offer() {
        this.channel.pipeline().addFirst(this.writableListener);
        this.session.sendPacket(this.packs.createInfoPacket(this.required));
    }

    @Override
    public PacketSignal handle(ResourcePackClientResponsePacket packet) {
        if (!this.channel.isActive() || this.phase == Phase.DONE) {
            return PacketSignal.HANDLED;
        }

        switch (packet.getStatus()) {
            case REFUSED -> this.reject();
            case SEND_PACKS -> this.requestPacks(packet);
            case HAVE_ALL_PACKS -> this.sendStack();
            case COMPLETED -> {
                if (this.phase != Phase.STACK_SENT) {
                    this.reject();
                    break;
                }

                this.finish();
                this.onComplete.run();
            }
        }

        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ResourcePackChunkRequestPacket packet) {
        if (!this.channel.isActive() || this.phase == Phase.DONE) {
            return PacketSignal.HANDLED;
        }

        CloudResourcePack pack = this.currentPack;
        int index = packet.getChunkIndex();
        if (pack == null || !pack.getId().equals(packet.getPackId())
                || !pack.getVersion().toString().equals(packet.getPackVersion())
                || index < 0 || index >= this.chunkCount) {
            this.reject();
        } else if (!this.requestedChunks.get(index)) {
            if (this.pendingChunks.size() >= MAX_PENDING_CHUNKS) {
                this.reject();
            } else {
                this.requestedChunks.set(index);
                this.pendingChunks.addLast(index);
                this.transferNextChunk();
            }
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ClientCacheStatusPacket packet) {
        this.onCacheStatus.accept(packet.isSupported());
        return PacketSignal.HANDLED;
    }

    private void requestPacks(ResourcePackClientResponsePacket packet) {
        if (this.phase != Phase.OFFERED || packet.getPackIds().isEmpty()) {
            return;
        }

        Set<UUID> uniquePacks = new HashSet<>();
        for (String id : packet.getPackIds()) {
            CloudResourcePack pack = this.packs.getPackByIdVersion(id);
            if (pack == null || !uniquePacks.add(pack.getId())) {
                this.reject();
                return;
            }

            this.requestedPacks.addLast(pack);
        }

        this.phase = Phase.TRANSFERRING;
        this.offerNextPack();
    }

    private void offerNextPack() {
        CloudResourcePack pack = this.requestedPacks.pollFirst();
        this.currentPack = pack;
        if (pack == null) {
            this.phase = Phase.TRANSFERRED;
            return;
        }

        this.requestedChunks.clear();
        this.chunksWritten = 0;
        this.chunkCount = Math.toIntExact(Math.ceilDiv(pack.getSize(), CHUNK_SIZE));

        ResourcePackDataInfoPacket info = new ResourcePackDataInfoPacket();
        info.setPackId(pack.getId());
        info.setPackVersion(pack.getVersion().toString());
        info.setMaxChunkSize(CHUNK_SIZE);
        info.setChunkCount(this.chunkCount);
        info.setCompressedPackSize(pack.getSize());
        info.setHash(pack.getHash());
        info.setType(ResourcePackType.RESOURCES);
        this.session.sendPacket(info);
    }

    private void transferNextChunk() {
        if (this.phase != Phase.TRANSFERRING || this.chunkInFlight || !this.channel.isActive()
                || !this.channel.isWritable()) {
            return;
        }

        Integer index = this.pendingChunks.pollFirst();
        if (index == null) {
            return;
        }

        CloudResourcePack pack = this.currentPack;
        this.chunkInFlight = true;
        if (!this.submitRead.test(() -> this.readChunk(pack, index))) {
            this.reject();
        }
    }

    @SuppressWarnings("resource") // The shared event loop is owned by the network.
    private void readChunk(CloudResourcePack pack, int index) {
        if (!this.channel.isActive()) {
            return;
        }

        long offset = (long) index * CHUNK_SIZE;
        byte[] chunk;
        try {
            chunk = pack.readChunk(offset, CHUNK_SIZE);
        } catch (IOException | IllegalArgumentException failure) {
            this.channel.eventLoop().execute(() -> {
                if (this.phase == Phase.TRANSFERRING && this.channel.isActive()) {
                    log.warn("Unable to read resource pack {}", pack.getId(), failure);
                    this.reject();
                }
            });
            return;
        }

        this.channel.eventLoop().execute(() -> this.writeChunk(pack, index, offset, chunk));
    }

    private void writeChunk(CloudResourcePack pack, int index, long offset, byte[] chunk) {
        if (this.phase != Phase.TRANSFERRING || !this.channel.isActive()) {
            return;
        }

        ResourcePackChunkDataPacket data = new ResourcePackChunkDataPacket();
        data.setPackId(pack.getId());
        data.setPackVersion(pack.getVersion().toString());
        data.setChunkIndex(index);
        data.setProgress(offset);
        data.setData(Unpooled.wrappedBuffer(chunk));

        this.channel.writeAndFlush(BedrockPacketWrapper.create(0, 0, 0, data, null)).addListener(future -> {
            if (this.phase != Phase.TRANSFERRING || !this.channel.isActive()) {
                return;
            }

            if (!future.isSuccess()) {
                this.reject();
                return;
            }

            this.chunkInFlight = false;
            if (++this.chunksWritten == this.chunkCount) {
                this.offerNextPack();
            }
            this.transferNextChunk();
        });
    }

    private void sendStack() {
        if (this.phase == Phase.STACK_SENT) {
            return;
        }

        if (this.phase != Phase.OFFERED && this.phase != Phase.TRANSFERRED) {
            this.reject();
            return;
        }

        this.phase = Phase.STACK_SENT;
        this.session.sendPacket(this.packs.createStackPacket(this.required));
    }

    private void reject() {
        this.finish();
        this.peer.disconnectSession(this.session, DisconnectFailReason.RESOURCE_PACK_PROBLEM, Component.translatable("disconnectionScreen.resourcePack"));
    }

    private void finish() {
        this.phase = Phase.DONE;
        this.currentPack = null;
        this.pendingChunks.clear();
        this.requestedPacks.clear();
        this.requestedChunks.clear();
        this.channel.pipeline().remove(this.writableListener);
    }

    private enum Phase {
        OFFERED,
        TRANSFERRING,
        TRANSFERRED,
        STACK_SENT,
        DONE
    }
}
