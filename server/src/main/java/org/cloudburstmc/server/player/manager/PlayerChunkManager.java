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
public final class PlayerChunkManager {

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
     * subChunkLimit is zero (entirely air columns) are promoted here
     * immediately when the shell is sent, because the client will not
     * send any sub-chunk requests for them.
     */
    private final LongSet readyChunks = new LongOpenHashSet();

    /**
     * Outstanding sub-chunk section count per chunk key.
     * <p>
     * When a shell is dispatched with subChunkLimit N the client will request
     * N sections in total. That count is stored here when the shell is sent. Each call to
     * {@link #recordSubChunkServed} decrements it by the number of sections
     * answered in that response. When the count reaches zero the key is
     * removed and the chunk is promoted to {@link #readyChunks}.
     * <p>
     * Chunks with a zero subChunkLimit are entirely air so the client
     * sends no section requests for them. Those chunks go directly to
     * readyChunks when the shell is sent.
     */
    private final Long2IntMap pendingSubChunks = new Long2IntOpenHashMap();

    /**
     * Chunk keys inside the view radius whose load/serialize work has not
     * started yet. This keeps one join from scheduling the entire view radius
     * into generation at once.
     */
    private final LongList loadQueue = new LongArrayList();

    /**
     * Pending serialized shell packets waiting to be sent, keyed by chunk
     * key. A {@code null} value means async serialization is still in
     * flight. The entry is present from the moment serialization starts
     * until the packet has been dispatched in {@link #sendQueued}.
     */
    private final Long2ObjectMap<LevelChunkPacket> sendQueue = new Long2ObjectOpenHashMap<>();

    private final LongSet retainedChunks = new LongOpenHashSet();

    private final LongComparator distanceSorter = this::compareDistanceToPlayer;
    private final AtomicLong chunksSentCounter = new AtomicLong();
    private final LongConsumer removeChunkView;

    private volatile int chunkRadius;

    private int lastQueuedChunkX = Integer.MIN_VALUE;
    private int lastQueuedChunkZ = Integer.MIN_VALUE;
    private int lastQueuedChunkRadius = Integer.MIN_VALUE;

    public PlayerChunkManager(CloudPlayer player) {
        this.player = player;
        this.removeChunkView = key -> this.updateEntityViewsInChunk(key, EntityViewUpdate.DESPAWN);
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
            this.release(this.sendQueue.remove(key));
            this.releaseChunk(key);
            this.removeChunkView.accept(key);
        }

        this.scheduleQueuedChunkLoads(chunksPerTick);

        LongList list = new LongArrayList(this.sendQueue.keySet());

        try (Timing ignored = Timings.playerChunkOrderTimer.startTiming()) {
            list.unstableSort(this.distanceSorter);
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
                if (!this.player.sendPacket(packet)) {
                    this.release(packet);
                    continue;
                }

                int subChunkLimit = packet.getSubChunkLimit();
                this.shellSentChunks.add(key);

                if (subChunkLimit <= 0) {
                    this.readyChunks.add(key);
                    this.spawnEntityViewsInChunk(key);

                    CloudChunk chunk = this.player.getLevel().getLoadedChunk(key);
                    checkArgument(
                            chunk != null,
                            "Attempted to send unloaded chunk (%s, %s) to %s",
                            CloudChunk.fromKeyX(key),
                            CloudChunk.fromKeyZ(key),
                            this.player.getName()
                    );
                } else {
                    int pending = subChunkLimit;
                    this.pendingSubChunks.put(key, pending);
                }

                chunksPerTick--;
                this.chunksSentCounter.incrementAndGet();
            }
        }

        this.scheduleQueuedChunkLoads(chunksPerTick);
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

        this.spawnEntityViewsInChunk(key);
    }

    public synchronized void despawnVisibleEntities() {
        this.updateEntityViewsInChunks(this.viewChunks, EntityViewUpdate.DESPAWN);
    }

    public synchronized void spawnReadyEntities() {
        this.updateEntityViewsInChunks(this.readyChunks, EntityViewUpdate.SPAWN);
    }

    public synchronized void refreshReadyEntities() {
        this.updateEntityViewsInChunks(this.readyChunks, EntityViewUpdate.REFRESH);
    }

    public synchronized void spawnReadyEntitiesIn(CloudChunk chunk) {
        if (chunk == null) {
            return;
        }

        long key = chunk.key();
        if (this.readyChunks.contains(key)) {
            this.updateEntityViewsInChunk(key, EntityViewUpdate.SPAWN);
        }
    }

    private void spawnEntityViewsInChunk(long key) {
        this.updateEntityViewsInChunk(key, EntityViewUpdate.SPAWN);
    }

    private void updateEntityViewsInChunks(LongCollection chunks, EntityViewUpdate update) {
        LongList chunkSnapshot = new LongArrayList(chunks);
        for (long key : chunkSnapshot) {
            this.updateEntityViewsInChunk(key, update);
        }
    }

    private void updateEntityViewsInChunk(long key, EntityViewUpdate update) {
        CloudChunk chunk = this.player.getLevel().getLoadedChunk(key);
        if (chunk == null) {
            return;
        }

        for (Entity entity : chunk.getEntities()) {
            this.updateEntityViewIfVisible(entity, update);
        }

        for (CloudPlayer player : chunk.getPlayers()) {
            this.updateEntityViewIfVisible(player, update);
        }
    }

    private boolean canUpdateEntityView(Entity entity, EntityViewUpdate update) {
        if (entity == this.player) {
            return false;
        }
        return update == EntityViewUpdate.DESPAWN || !entity.isClosed() && entity.isAlive();
    }

    private void updateEntityViewIfVisible(Entity entity, EntityViewUpdate update) {
        if (this.canUpdateEntityView(entity, update)) {
            this.updateEntityView(entity, update);
        }
    }

    private void updateEntityView(Entity entity, EntityViewUpdate update) {
        switch (update) {
            case SPAWN -> entity.spawnTo(this.player);
            case DESPAWN -> entity.despawnFrom(this.player);
            case REFRESH -> {
                entity.despawnFrom(this.player);
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
        int radius = this.chunkRadius;
        if (chunkX == this.lastQueuedChunkX && chunkZ == this.lastQueuedChunkZ && radius == this.lastQueuedChunkRadius) {
            return;
        }

        this.lastQueuedChunkX = chunkX;
        this.lastQueuedChunkZ = chunkZ;
        this.lastQueuedChunkRadius = radius;

        int radiusSqr = radius * radius;

        LongList chunksToLoad = new LongArrayList();

        for (int x = -radius; x <= radius; ++x) {
            for (int z = -radius; z <= radius; ++z) {
                if ((x * x) + (z * z) > radiusSqr) {
                    continue;
                }

                int cx = chunkX + x;
                int cz = chunkZ + z;
                long key = CloudChunk.key(cx, cz);

                if (this.viewChunks.add(key)) {
                    chunksToLoad.add(key);
                }
            }
        }

        LongList chunksToRemove = new LongArrayList();
        for (long key : this.viewChunks) {
            int dx = CloudChunk.fromKeyX(key) - chunkX;
            int dz = CloudChunk.fromKeyZ(key) - chunkZ;
            if ((dx * dx) + (dz * dz) > radiusSqr) {
                chunksToRemove.add(key);
            }
        }

        for (long key : chunksToRemove) {
            this.viewChunks.remove(key);
            this.shellSentChunks.remove(key);
            this.readyChunks.remove(key);
            this.pendingSubChunks.remove(key);
            this.removeFromView(key);
        }

        for (int i = this.loadQueue.size() - 1; i >= 0; i--) {
            if (!this.viewChunks.contains(this.loadQueue.getLong(i))) {
                this.loadQueue.removeLong(i);
            }
        }

        if (!chunksToRemove.isEmpty() || !chunksToLoad.isEmpty()) {
            NetworkChunkPublisherUpdatePacket publisherPacket = new NetworkChunkPublisherUpdatePacket();
            publisherPacket.setPosition(this.player.getPosition().toInt());
            publisherPacket.setRadius(this.chunkRadius << 4);
            this.player.sendPacket(publisherPacket);
        }

        chunksToLoad.unstableSort(this.distanceSorter);

        for (long key : chunksToLoad) {
            if (!this.loadQueue.contains(key)) {
                this.loadQueue.add(key);
            }
        }

        this.scheduleQueuedChunkLoads(this.player.getServer().getConfig().getChunkSending().getPerTick());
    }

    private void scheduleQueuedChunkLoads(int chunksPerTick) {
        if (chunksPerTick <= 0 || this.loadQueue.isEmpty()) {
            return;
        }

        int maxActive = Math.max(chunksPerTick * 4, chunksPerTick);
        int loadsToStart = Math.min(chunksPerTick, maxActive - this.sendQueue.size());
        while (loadsToStart > 0 && !this.loadQueue.isEmpty()) {
            long key = this.loadQueue.removeLong(0);
            if (!this.viewChunks.contains(key) || this.shellSentChunks.contains(key) || this.sendQueue.containsKey(key)) {
                continue;
            }

            this.startChunkLoad(key);
            loadsToStart--;
        }
    }

    private void startChunkLoad(long key) {
        final int cx = CloudChunk.fromKeyX(key);
        final int cz = CloudChunk.fromKeyZ(key);

        if (this.sendQueue.containsKey(key)) {
            return;
        }
        this.sendQueue.put(key, null);
        this.retainChunk(key);

        Executor asyncExecutor = ((CloudAsyncScheduler) this.player.getServer().getAsyncScheduler()).getExecutor();
        this.player.getLevel().getChunkFuture(cx, cz)
                .thenApplyAsync(chunk -> {
                    synchronized (PlayerChunkManager.this) {
                        return this.viewChunks.contains(key) && this.sendQueue.containsKey(key) ? chunk : null;
                    }
                }, asyncExecutor)
                .thenApplyAsync(
                        chunk -> chunk == null ? null : chunk.createChunkPacket(),
                        asyncExecutor
                )
                .whenCompleteAsync((packet, throwable) -> {
                    synchronized (PlayerChunkManager.this) {
                        if (throwable != null) {
                            if (this.sendQueue.remove(key, null)) {
                                this.viewChunks.remove(key);
                                this.removeFromView(key);
                                this.invalidateQueuedCenter();
                            }
                            log.error("Unable to create chunk packet for {}", this.player.getName(), throwable);
                        } else if (packet == null) {
                            this.sendQueue.remove(key, null);
                        } else if (!this.sendQueue.replace(key, null, packet)) {
                            this.release(packet);
                            if (this.sendQueue.containsKey(key)) {
                                log.warn("Chunk ({},{}) already queued for {}, dropping duplicate", cx, cz, this.player.getName());
                            }
                        }
                    }
                }, asyncExecutor);
    }

    private void release(LevelChunkPacket packet) {
        if (packet != null && packet.refCnt() > 0) {
            packet.release();
        }
    }

    private void removeFromView(long key) {
        for (int i = this.loadQueue.size() - 1; i >= 0; i--) {
            if (this.loadQueue.getLong(i) == key) {
                this.loadQueue.removeLong(i);
            }
        }
        if (this.sendQueue.containsKey(key)) {
            this.release(this.sendQueue.remove(key));
        }
        this.releaseChunk(key);
        this.removeChunkView.accept(key);
    }

    private void retainChunk(long key) {
        if (this.retainedChunks.add(key)) {
            this.player.getLevel().addPlayerViewChunkTicket(key, this.player);
        }
    }

    private void releaseChunk(long key) {
        if (this.retainedChunks.remove(key)) {
            this.player.getLevel().removePlayerViewChunkTicket(key, this.player);
        }
    }

    public int getChunkRadius() {
        return this.chunkRadius;
    }

    public void setChunkRadius(int chunkRadius) {
        chunkRadius = GenericMath.clamp(
                chunkRadius,
                8,
                this.getMaxChunkRadius()
        );

        if (this.chunkRadius != chunkRadius) {
            this.chunkRadius = chunkRadius;
            ChunkRadiusUpdatedPacket packet = new ChunkRadiusUpdatedPacket();
            packet.setRadius(chunkRadius);
            this.player.sendPacket(packet);
            this.queueNewChunks();
        }
    }

    public int getLoadedChunkRadius() {
        return this.chunkRadius;
    }

    private int getMaxChunkRadius() {
        return Math.max(8, Math.min(
                this.player.getServer().getConfig().getChunkSending().getMaxChunkRadius(),
                this.player.getServer().getConfig().getChunkSending().getMaxLoadedChunkRadius()
        ));
    }

    private int compareDistanceToPlayer(long a, long b) {
        int centerX = this.player.getPosition().getFloorX() >> 4;
        int centerZ = this.player.getPosition().getFloorZ() >> 4;
        int ax = CloudChunk.fromKeyX(a) - centerX;
        int az = CloudChunk.fromKeyZ(a) - centerZ;
        int bx = CloudChunk.fromKeyX(b) - centerX;
        int bz = CloudChunk.fromKeyZ(b) - centerZ;
        return Integer.compare(ax * ax + az * az, bx * bx + bz * bz);
    }

    /**
     * Returns {@code true} if all sub-chunk sections for the chunk at
     * {@code (x, z)} have been served to this client.
     * <p>
     * This is the correct gate for movement validation. A chunk whose
     * shell has been sent but whose sub-chunk exchanges are still in
     * progress is not yet walkable.
     */
    public boolean isChunkSent(int x, int z) {
        return this.isChunkSent(CloudChunk.key(x, z));
    }

    public synchronized boolean isChunkSent(long key) {
        return this.readyChunks.contains(key);
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
        if (this.viewChunks.remove(key)) {
            this.removeFromView(key);
        }
        this.shellSentChunks.remove(key);
        this.readyChunks.remove(key);
        this.pendingSubChunks.remove(key);
        this.invalidateQueuedCenter();
    }

    public void prepareRegion(Vector3f pos) {
        this.prepareRegion(pos.getFloorX() >> 4, pos.getFloorZ() >> 4);
    }

    public void prepareRegion(int chunkX, int chunkZ) {
        this.clear();
        this.queueNewChunks(chunkX, chunkZ);
    }

    public synchronized void clear() {
        LongList pendingSends = new LongArrayList(this.sendQueue.keySet());
        for (long key : pendingSends) {
            this.release(this.sendQueue.remove(key));
        }
        this.loadQueue.clear();
        this.viewChunks.forEach((LongConsumer) this::removeFromView);
        this.viewChunks.clear();
        this.shellSentChunks.clear();
        this.readyChunks.clear();
        this.pendingSubChunks.clear();
        this.invalidateQueuedCenter();
    }

    private void invalidateQueuedCenter() {
        this.lastQueuedChunkX = Integer.MIN_VALUE;
        this.lastQueuedChunkZ = Integer.MIN_VALUE;
        this.lastQueuedChunkRadius = Integer.MIN_VALUE;
    }
}
