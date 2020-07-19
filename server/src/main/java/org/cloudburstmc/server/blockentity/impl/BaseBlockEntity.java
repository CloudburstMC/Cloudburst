package org.cloudburstmc.server.blockentity.impl;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author MagicDroidX
 */
@Log4j2
public abstract class BaseBlockEntity implements BlockEntity {

    public static AtomicLong ID_ALLOCATOR = new AtomicLong(1);
    public final long id;
    private final BlockEntityType<?> type;
    private final Vector3i position;
    private final Chunk chunk;
    private final Level level;
    public boolean movable = true;
    public boolean closed = false;
    protected long lastUpdate;
    protected Server server;
    protected Timing timing;
    private String customName;
    private boolean justCreated = true;

    public BaseBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        checkNotNull(type, "type");
        checkNotNull(chunk, "chunk");
        checkNotNull(position, "position");

        this.type = type;
        this.timing = Timings.getBlockEntityTiming(this);
        this.server = chunk.getLevel().getServer();
        this.position = position;
        this.chunk = chunk;
        this.level = chunk.getLevel();
        this.lastUpdate = System.currentTimeMillis();
        this.id = ID_ALLOCATOR.getAndIncrement();

        this.chunk.addBlockEntity(this);
        this.level.addBlockEntity(this);

        this.init();

        this.scheduleUpdate();
    }

    @Override
    public BlockEntityType<?> getType() {
        return this.type;
    }

    protected void init() {
    }

    public long getId() {
        return id;
    }

    public void loadAdditionalData(NbtMap tag) {
        tag.listenForBoolean("isMovable", this::setMovable);
        tag.listenForString("CustomName", this::setCustomName);
    }

    public void saveAdditionalData(NbtMapBuilder tag) {
        tag.putBoolean("isMovable", this.movable);
        if (this.customName != null) {
            tag.putString("CustomName", this.customName);
        }

        this.saveClientData(tag);
    }

    /**
     * NBT data that is specifically sent to the client
     *
     * @param tag tag to write data to
     */
    protected void saveClientData(NbtMapBuilder tag) {

    }

    public final NbtMap getItemTag() {
        return this.getTag(false, false, true);
    }

    @Override
    public final NbtMap getServerTag() {
        return getTag(true, true, true);
    }

    @Override
    public final NbtMap getClientTag() {
        return getTag(true, false, false);
    }

    @Override
    public final NbtMap getChunkTag() {
        return getTag(true, true, false);
    }

    private NbtMap getTag(boolean id, boolean position, boolean server) {
        NbtMapBuilder tag = NbtMap.builder();

        if (id) {
            tag.putString("id", BlockEntityRegistry.get().getPersistentId(this.type));
        }

        if (position) {
            tag.putInt("x", this.position.getX());
            tag.putInt("y", this.position.getY());
            tag.putInt("z", this.position.getZ());
        }

        if (server) {
            this.saveAdditionalData(tag);
        } else {
            this.saveClientData(tag);
        }

        return tag.build();
    }

    @Override
    public abstract boolean isValid();

    public boolean onUpdate() {
        if (this.justCreated) {
            if (this.isSpawnable()) {
                this.spawnToAll();
            }

            this.justCreated = false;
        }

        return false;
    }

    public final void scheduleUpdate() {
        this.level.scheduleBlockEntityUpdate(this);
    }

    public void close() {
        if (!this.closed) {
            this.closed = true;
            if (this.chunk != null) {
                this.chunk.removeBlockEntity(this);
            }
            if (this.level != null) {
                this.level.removeBlockEntity(this);
            }
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public void onBreak() {

    }

    public void setDirty() {
        chunk.setDirty();
    }

    @Override
    public boolean isMovable() {
        return movable;
    }

    @Override
    public void setMovable(boolean moveble) {
        this.movable = moveble;
    }

    /**
     * Gets the name of this object.
     *
     * @return The name of this object.
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    @Nullable
    public final String getCustomName() {
        return customName;
    }

    /**
     * Changes the name of this object, or names it.
     *
     * @param customName The new name of this object.
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public final void setCustomName(@Nullable String customName) {
        this.customName = customName;
    }

    /**
     * Whether this object has a name.
     *
     * @return {@code true} for this object has a name.
     * @since Nukkit 1.0 | Nukkit API 1.0.0
     */
    public final boolean hasCustomName() {
        return this.customName != null;
    }

    public void spawnTo(Player player) {
        if (this.closed) {
            return;
        }

        BlockEntityDataPacket packet = new BlockEntityDataPacket();
        packet.setBlockPosition(this.getPosition());
        packet.setData(this.getClientTag());
        player.sendPacket(packet);
    }

    public void spawnToAll() {
        if (this.closed) {
            return;
        }

        for (Player player : this.getChunk().getPlayerLoaders()) {
            if (player.spawned) {
                this.spawnTo(player);
            }
        }
    }

    public final boolean updateFromClient(NbtMap tag, Player player) {
        if (!tag.getString("id").equals(BlockEntityRegistry.get().getPersistentId(this.getType()))) {
            return false;
        }
        return this.updateNbtMap(tag, player);
    }

    /**
     * Called when a player updates a block entity's NBT data
     * for example when writing on a sign.
     *
     * @param nbt    tag
     * @param player player
     * @return bool indication of success, will respawn the tile to the player if false.
     */
    protected boolean updateNbtMap(NbtMap nbt, Player player) {
        return false;
    }

    @Override
    public boolean isSpawnable() {
        return false;
    }

    @Override
    public Vector3i getPosition() {
        return position;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public BlockState getBlock() {
        return this.chunk.getBlock(this.position.getX() & 0xf, this.position.getY() & 0xff, this.position.getZ() & 0xf);
    }
}
