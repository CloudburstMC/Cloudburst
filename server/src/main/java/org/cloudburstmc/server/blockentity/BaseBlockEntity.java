package org.cloudburstmc.server.blockentity;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author MagicDroidX
 */
@Log4j2
public abstract class BaseBlockEntity implements BlockEntity {

    private static final Map<BlockEntityType<?>, NbtMap> tags = new HashMap<>();

    public static AtomicLong ID_ALLOCATOR = new AtomicLong(1);
    public final long id;
    private final BlockEntityType<?> type;
    private final Vector3i position;
    private final CloudChunk chunk;
    private final CloudLevel level;
    private NbtMap tag;
    public boolean movable = true;
    public boolean closed = false;
    protected long lastUpdate;
    protected CloudServer server;
    protected Timing timing;
    private String customName;
    private boolean justCreated = true;

    public BaseBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        checkNotNull(type, "type");
        checkNotNull(chunk, "chunk");
        checkNotNull(position, "position");

        this.type = type;
        this.timing = Timings.getBlockEntityTiming(this);
        this.server = (CloudServer) chunk.getLevel().getServer();
        this.position = position;
        this.chunk = (CloudChunk) chunk;
        this.level = (CloudLevel) chunk.getLevel();
        this.lastUpdate = System.currentTimeMillis();
        this.id = ID_ALLOCATOR.getAndIncrement();

        this.chunk.addBlockEntity(this);
        this.level.addBlockEntity(this);

        this.init();

        this.scheduleUpdate();
    }

    public NbtMap getTag() {
        return tag;
    }

    public CloudServer getServer() {
        return this.server;
    }

    public BlockEntityType<?> getType() {
        return this.type;
    }

    protected void init() {
    }

    public long getId() {
        return id;
    }

    public void loadAdditionalData(NbtMap tag) {
        this.tag = tag;
        tags.putIfAbsent(this.getType(), tag);

        tag.listenForBoolean("isMovable", this::setMovable);
        tag.listenForString("CustomName", this::setCustomName);
    }

    public void saveAdditionalData(NbtMapBuilder tag) {
        if (this.tag != null && !this.tag.isEmpty()) {
            tag.putAll(this.tag);
        }

        tag.putBoolean("isMovable", this.movable);
        if (this.customName != null) {
            tag.putString("CustomName", this.customName);
        }

        this.saveClientData(tag);

//        var nbt = tag.build();
//        checkNbt(this.tag, nbt);

//        log.info("saving: " + this.type);
//        tag.forEach((key, value) -> {
//            if (!this.tag.containsKey(key)) {
//                log.info("Redundant entry: (" + key + ", " + value + ")");
//                return;
//            }
//
//            var type = NbtType.byClass(value.getClass());
//            var vanillaType = NbtType.byClass(this.tag.get(key).getClass());
//
//            if (type != vanillaType) {
//                log.info("Incompatible types for '" + key + "' vanilla: " + vanillaType.getTypeName() + "  local: " + type.getTypeName());
//            }
//        });
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

    public final NbtMap getServerTag() {
        return getTag(true, true, true);
    }

    public final NbtMap getClientTag() {
        return getTag(true, false, false);
    }

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

//            log.info("saving: " + this.type);
//            if (!nbtEquals(this.tag, tag.build())) {
//                log.info("not equal NBT: " + this.type);
//            }
//
//            checkNbt(this.tag, tag.build());
//
//            log.info("vanilla:");
//            log.info(NbtUtils.toString(this.tag));
//            log.info("\n\n\nlocal:");
//            log.info(NbtUtils.toString(tag.build()));
//            log.info("\n\n\n");
        } else {
            this.saveClientData(tag);
        }

        return tag.build();
    }

    private boolean nbtEquals(Object tag, Object local) {
        if (tag instanceof Map) {
            if (!(local instanceof Map)) {
                return false;
            }

            var localMap = (Map<String, ?>) local;
            var vanillaMap = (Map<String, ?>) tag;

            for (Entry<String, ?> entry : localMap.entrySet()) {
                if (!vanillaMap.containsKey(entry.getKey())) {
                    return false;
                }

                if (!nbtEquals(vanillaMap.get(entry.getKey()), entry.getValue())) {
                    return false;
                }
            }

            return true;
        }

        if (tag instanceof List) {
            if (!(local instanceof List)) {
                return false;
            }

            var localList = (List) local;
            var vanillaList = (List) tag;

            if (localList.size() != vanillaList.size()) {
                return false;
            }

            for (int i = 0; i < localList.size(); i++) {
                var localItem = localList.get(i);
                var vanillaItem = vanillaList.get(i);

                if (!nbtEquals(vanillaItem, localItem)) {
                    return false;
                }
            }

            return true;
        }

        return tag.getClass() == local.getClass() && Objects.equals(tag, local);
    }

    private boolean checkNbt(NbtMap tag, NbtMap local) {
        for (Entry<String, Object> entry : local.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();

            if (!tag.containsKey(key)) {
                log.info("redundant key: " + key);
                continue;
            }

            var vanilla = tag.get(key);

            var localType = NbtType.byClass(value.getClass());
            var vanillaType = NbtType.byClass(vanilla.getClass());

            if (vanillaType != localType) {
                log.info("wrong tag types, local: " + localType.getTypeName() + ", vanilla: " + vanillaType.getTypeName());
                log.info("values: local: " + value + ", vanilla: " + vanilla);
                return false;
            }

            if (value instanceof NbtMap) {
                if (!checkNbt((NbtMap) vanilla, (NbtMap) value)) {
                    log.info("key - " + key);
                    return false;
                }

                return true;
            }

            if (value instanceof List<?>) {
                if (!checkList((List<?>) vanilla, (List<?>) value)) {
                    log.info("key - " + key);
                    return false;
                }

                return true;
            }
        }

        return true;
    }

    private boolean checkList(List<?> tag, List<?> local) {
        for (int i = 0; i < local.size() && i < tag.size(); i++) {
            Object value = local.get(i);
            Object vanilla = tag.get(i);

            var localType = NbtType.byClass(value.getClass());
            var vanillaType = NbtType.byClass(vanilla.getClass());

            if (vanillaType != localType) {
                log.info("wrong tag types at index '" + i + "', local: " + localType.getTypeName() + ", vanilla: " + vanillaType.getTypeName());
                log.info("values: local: " + value + ", vanilla: " + vanilla);
                return false;
            }

            if (value instanceof NbtMap) {
                return checkNbt((NbtMap) vanilla, (NbtMap) value);
            }

            if (value instanceof List<?>) {
                return checkList((List<?>) vanilla, (List<?>) value);
            }
        }

        return true;
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
        ((CloudPlayer) player).sendPacket(packet);
    }

    public void spawnToAll() {
        if (this.closed) {
            return;
        }

        for (CloudPlayer player : this.getChunk().getPlayerLoaders()) {
            if (player.spawned) {
                this.spawnTo(player);
            }
        }
    }

    public final boolean updateFromClient(NbtMap tag, CloudPlayer player) {
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
    protected boolean updateNbtMap(NbtMap nbt, CloudPlayer player) {
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
    public CloudLevel getLevel() {
        return level;
    }

    public CloudChunk getChunk() {
        return chunk;
    }

    public Block getBlock() {
        return this.level.getBlock(this.position);
    }

    @Override
    public BlockState getBlockState() {
        return this.level.getBlockState(this.position);
    }
}
