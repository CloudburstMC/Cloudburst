package org.cloudburstmc.server.player.manager;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import it.unimi.dsi.fastutil.longs.*;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.scheduler.CloudAsyncScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import static com.google.common.base.Preconditions.checkArgument;

@Log4j2
public class PlayerChunkManager {

    private final CloudPlayer player;

    /**
     * All chunk keys within the current view radius.
     * <p>
     * Populated immediately when a chunk enters the radius in
     * {@link #queueNewChunks}. Drives radius-change tracking,
     * {@link NetworkChunkPublisherUpdatePacket} sends, and out-of-radius
     * unloading. A key may be in this set long before the terrain packet
     * has been sent.
     */
    private final LongSet viewChunks = new LongOpenHashSet();

    /**
     * Chunk keys whose {@link LevelChunkPacket} biome shell has been
     * dispatched to the client.
     * <p>
     * In sub-chunk request mode the shell carries only biome data and no
     * block data. The server always uses sub-chunk request mode, so a key
     * here does NOT mean the client has walkable terrain. Use
     * {@link #readyChunks} for movement and entity-spawn gates.
     */
    private final LongSet shellSentChunks = new LongOpenHashSet();

    /**
     * Chunk keys for which all sub-chunk sections have been served to the
     * client.
     * <p>
     * This is the authoritative gate for both movement validation and
     * entity spawning. A chunk enters this set when
     * {@link #recordSubChunkServed} determines that the outstanding
     * section count for the column has reached zero. Chunks whose
     * subChunkLimit is negative (entirely air columns) are promoted here
     * immediately when the shell is sent, because the client will not
     * send any sub-chunk requests for them.
     */
    private final LongSet readyChunks = new LongOpenHashSet();

    /**
     * Outstanding sub-chunk section count per chunk key.
     * <p>
     * When a shell is dispatched with subChunkLimit N the client will request
     * sections 0 through N inclusive, which is N + 1 requests in total.
     * That count is stored here when the shell is sent. Each call to
     * {@link #recordSubChunkServed} decrements it by the number of sections
     * answered in that response. When the count reaches zero the key is
     * removed and the chunk is promoted to {@link #readyChunks}.
     * <p>
     * Chunks with a negative subChunkLimit are entirely air so the client
     * sends no section requests for them. Those chunks go directly to
     * readyChunks when the shell is sent.
     */
    private final Long2IntMap pendingSubChunks = new Long2IntOpenHashMap();

    /**
     * Pending serialized shell packets waiting to be sent, keyed by chunk
     * key. A {@code null} value means async serialization is still in
     * flight. The entry is present from the moment serialization starts
     * until the packet has been dispatched in {@link #sendQueued}.
     */
    private final Long2ObjectMap<LevelChunkPacket> sendQueue = new Long2ObjectOpenHashMap<>();

    private final AtomicLong chunksSentCounter = new AtomicLong();
    private final LongConsumer removeChunkLoader;
    private volatile int radius;

    public PlayerChunkManager(CloudPlayer player) {
        this.player = player;
        this.removeChunkLoader = chunkKey -> {
            CloudChunk chunk = this.player.getLevel().getLoadedChunk(chunkKey);
            if (chunk != null) {
                chunk.removeLoader(this.player);
                for (Entity entity : chunk.getEntities()) {
                    entity.despawnFrom(this.player);
                }
            }
        };
    }

    /**
     * Dispatch up to {@code perTick} queued shell packets to the client.
     * <p>
     * Called from {@link CloudPlayer#onUpdate} on the server tick thread.
     * Packets whose key has been removed from {@link #viewChunks} between
     * serialization completing and this tick are discarded without sending.
     */
    public synchronized void sendQueued() {
        int chunksPerTick = this.player.getServer().getConfig().getChunkSending().getPerTick();

        LongList keysToDiscard = new LongArrayList();
        for (Long2ObjectMap.Entry<LevelChunkPacket> entry : this.sendQueue.long2ObjectEntrySet()) {
            if (!this.viewChunks.contains(entry.getLongKey())) {
                keysToDiscard.add(entry.getLongKey());
            }
        }

        for (long key : keysToDiscard) {
            this.sendQueue.remove(key);
            this.removeChunkLoader.accept(key);
        }

        int centerX = this.player.getPosition().getFloorX() >> 4;
        int centerZ = this.player.getPosition().getFloorZ() >> 4;

        LongList list = new LongArrayList(this.sendQueue.keySet());

        try (Timing ignored = Timings.playerChunkOrderTimer.startTiming()) {
            list.unstableSort((a, b) -> {
                int ax = CloudChunk.fromKeyX(a) - centerX;
                int az = CloudChunk.fromKeyZ(a) - centerZ;
                int bx = CloudChunk.fromKeyX(b) - centerX;
                int bz = CloudChunk.fromKeyZ(b) - centerZ;
                return Integer.compare(ax * ax + az * az, bx * bx + bz * bz);
            });
        }

        try (Timing ignored = Timings.playerChunkSendTimer.startTiming()) {
            for (long key : list.toLongArray()) {
                if (chunksPerTick <= 0) {
                    break;
                }

                LevelChunkPacket packet = this.sendQueue.get(key);
                if (packet == null) {
                    continue;
                }

                this.sendQueue.remove(key);
                this.player.sendPacket(packet);

                int subChunkLimit = packet.getSubChunkLimit();
                this.shellSentChunks.add(key);

                if (subChunkLimit < 0) {
                    this.readyChunks.add(key);

                    CloudChunk chunk = this.player.getLevel().getLoadedChunk(key);
                    checkArgument(
                            chunk != null,
                            "Attempted to send unloaded chunk (%s, %s) to %s",
                            CloudChunk.fromKeyX(key),
                            CloudChunk.fromKeyZ(key),
                            this.player.getName()
                    );

                    for (Entity entity : chunk.getEntities()) {
                        if (entity != this.player && !entity.isClosed() && entity.isAlive()) {
                            entity.spawnTo(this.player);
                        }
                    }
                } else {
                    int pending = subChunkLimit + 1;
                    this.pendingSubChunks.put(key, pending);
                }

                chunksPerTick--;
                this.chunksSentCounter.incrementAndGet();
            }
        }
    }

    /**
     * Called by the sub-chunk request handler after serving {@code sectionsServed}
     * sections for the chunk column at {@code (chunkX, chunkZ)}.
     * <p>
     * When the outstanding count for the column reaches zero the chunk is
     * promoted to {@link #readyChunks} and entities in it are spawned to
     * the player.
     * <p>
     * This method is called from the packet-handling thread and acquires
     * the manager lock to keep all state mutations consistent.
     */
    public synchronized void recordSubChunkServed(int chunkX, int chunkZ, int sectionsServed) {
        long key = CloudChunk.key(chunkX, chunkZ);

        if (!this.pendingSubChunks.containsKey(key)) {
            return;
        }

        int remaining = this.pendingSubChunks.get(key) - sectionsServed;
        if (remaining > 0) {
            this.pendingSubChunks.put(key, remaining);
            return;
        }

        this.pendingSubChunks.remove(key);
        this.readyChunks.add(key);

        CloudChunk chunk = this.player.getLevel().getLoadedChunk(key);
        if (chunk == null) {
            return;
        }

        for (Entity entity : chunk.getEntities()) {
            if (entity != this.player && !entity.isClosed() && entity.isAlive()) {
                entity.spawnTo(this.player);
            }
        }
    }

    public void queueNewChunks() {
        this.queueNewChunks(this.player.getPosition());
    }

    public void queueNewChunks(Vector3f pos) {
        this.queueNewChunks(pos.getFloorX() >> 4, pos.getFloorZ() >> 4);
    }

    public synchronized void queueNewChunks(int chunkX, int chunkZ) {
        int radius = this.getChunkRadius();
        int radiusSqr = radius * radius;

        LongSet chunksForRadius = new LongOpenHashSet();
        LongSet previousView = new LongOpenHashSet(this.viewChunks);
        LongList chunksToLoad = new LongArrayList();

        for (int x = -radius; x <= radius; ++x) {
            for (int z = -radius; z <= radius; ++z) {
                if ((x * x) + (z * z) > radiusSqr) {
                    continue;
                }

                int cx = chunkX + x;
                int cz = chunkZ + z;
                long key = CloudChunk.key(cx, cz);

                chunksForRadius.add(key);
                if (this.viewChunks.add(key)) {
                    chunksToLoad.add(key);
                }
            }
        }

        boolean viewChanged = this.viewChunks.retainAll(chunksForRadius);

        this.shellSentChunks.retainAll(this.viewChunks);
        this.readyChunks.retainAll(this.viewChunks);
        this.pendingSubChunks.keySet().retainAll(this.viewChunks);

        if (viewChanged || !chunksToLoad.isEmpty()) {
            NetworkChunkPublisherUpdatePacket publisherPacket = new NetworkChunkPublisherUpdatePacket();
            publisherPacket.setPosition(this.player.getPosition().toInt());
            publisherPacket.setRadius(this.radius);
            this.player.sendPacket(publisherPacket);
        }

        chunksToLoad.unstableSort((a, b) -> {
            int ax = CloudChunk.fromKeyX(a) - chunkX;
            int az = CloudChunk.fromKeyZ(a) - chunkZ;
            int bx = CloudChunk.fromKeyX(b) - chunkX;
            int bz = CloudChunk.fromKeyZ(b) - chunkZ;
            return Integer.compare(ax * ax + az * az, bx * bx + bz * bz);
        });

        for (long key : chunksToLoad.toLongArray()) {
            final int cx = CloudChunk.fromKeyX(key);
            final int cz = CloudChunk.fromKeyZ(key);

            if (this.sendQueue.putIfAbsent(key, null) == null) {
                Executor asyncExecutor = ((CloudAsyncScheduler) this.player.getServer().getAsyncScheduler()).getExecutor();
                this.player.getLevel().getChunkFuture(cx, cz)
                        .thenApplyAsync(chunk -> {
                            chunk.addLoader(this.player);
                            return chunk;
                        }, asyncExecutor)
                        .thenApplyAsync(
                                CloudChunk::createChunkPacket,
                                asyncExecutor
                        )
                        .whenCompleteAsync((packet, throwable) -> {
                            synchronized (PlayerChunkManager.this) {
                                if (throwable != null) {
                                    if (this.sendQueue.remove(key, null)) {
                                        this.viewChunks.remove(key);
                                    }
                                    log.error(
                                            "Unable to create chunk packet for {}",
                                            this.player.getName(),
                                            throwable
                                    );
                                } else if (!this.sendQueue.replace(key, null, packet)) {
                                    if (this.sendQueue.containsKey(key)) {
                                        log.warn(
                                                "Chunk ({},{}) already queued for {}, dropping duplicate",
                                                cx, cz,
                                                this.player.getName()
                                        );
                                    }
                                }
                            }
                        }, asyncExecutor);
            }
        }

        previousView.removeAll(chunksForRadius);
        previousView.forEach(this.removeChunkLoader);
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        if (this.radius != radius) {
            this.radius = radius;
            ChunkRadiusUpdatedPacket packet = new ChunkRadiusUpdatedPacket();
            packet.setRadius(radius >> 4);
            this.player.sendPacket(packet);
            this.queueNewChunks();
        }
    }

    public int getChunkRadius() {
        return this.radius >> 4;
    }

    public void setChunkRadius(int chunkRadius) {
        chunkRadius = GenericMath.clamp(
                chunkRadius,
                8,
                this.player.getServer().getConfig().getChunkSending().getMaxChunkRadius()
        );
        this.setRadius(chunkRadius << 4);
    }

    /**
     * Returns {@code true} if all sub-chunk sections for the chunk at
     * {@code (x, z)} have been served to this client.
     * <p>
     * This is the correct gate for movement validation and entity
     * spawning. A chunk whose shell has been sent but whose sub-chunk
     * exchanges are still in progress is not yet walkable.
     */
    public boolean isChunkSent(int x, int z) {
        return this.isChunkSent(CloudChunk.key(x, z));
    }

    public synchronized boolean isChunkSent(long key) {
        return this.readyChunks.contains(key);
    }

    /**
     * Returns {@code true} if the {@link LevelChunkPacket} biome shell
     * has been dispatched for the chunk at {@code (x, z)}.
     * <p>
     * The sub-chunk exchange may still be in progress. Use
     * {@link #isChunkSent} when walkable terrain is required.
     */
    public boolean isChunkShellSent(int x, int z) {
        return this.isChunkShellSent(CloudChunk.key(x, z));
    }

    public synchronized boolean isChunkShellSent(long key) {
        return this.shellSentChunks.contains(key);
    }

    /**
     * Returns {@code true} if the chunk key is within the current view
     * radius. The chunk may not have had any packet sent yet.
     */
    public boolean isChunkInView(int x, int z) {
        return this.isChunkInView(CloudChunk.key(x, z));
    }

    public synchronized boolean isChunkInView(long key) {
        return this.viewChunks.contains(key);
    }

    public long getChunksSent() {
        return this.chunksSentCounter.get();
    }

    /**
     * Unmodifiable view of chunks within the current view radius.
     * Includes chunks whose packets have not yet been sent.
     */
    public LongSet getViewChunks() {
        return LongSets.unmodifiable(this.viewChunks);
    }

    /**
     * Unmodifiable view of chunks for which all sub-chunk sections have
     * been served. These are chunks the client can walk in.
     */
    public LongSet getReadyChunks() {
        return LongSets.unmodifiable(this.readyChunks);
    }

    public synchronized void resendChunk(int chunkX, int chunkZ) {
        long key = CloudChunk.key(chunkX, chunkZ);
        this.viewChunks.remove(key);
        this.shellSentChunks.remove(key);
        this.readyChunks.remove(key);
        this.pendingSubChunks.remove(key);
        this.removeChunkLoader.accept(key);
    }

    public void prepareRegion(Vector3f pos) {
        this.prepareRegion(pos.getFloorX() >> 4, pos.getFloorZ() >> 4);
    }

    public void prepareRegion(int chunkX, int chunkZ) {
        this.clear();
        this.queueNewChunks(chunkX, chunkZ);
    }

    public synchronized void clear() {
        this.sendQueue.clear();
        this.viewChunks.forEach(this.removeChunkLoader);
        this.viewChunks.clear();
        this.shellSentChunks.clear();
        this.readyChunks.clear();
        this.pendingSubChunks.clear();
    }
}
