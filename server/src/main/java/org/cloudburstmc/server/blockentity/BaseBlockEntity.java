package org.cloudburstmc.server.blockentity;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.persistence.PersistentDataContainer;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.blockentity.data.CloudBlockEntityData;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.nbt.client.ClientNbtWriter;
import org.cloudburstmc.server.persistence.CloudPersistentDataContainer;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BaseBlockEntity implements BlockEntity {

    private static final Set<String> SAVE_METADATA = Set.of(
            "id", "x", "y", "z", CloudPersistentDataContainer.STORAGE_KEY
    );

    @Getter
    private final BlockEntityType<?> type;
    @Getter
    private final Vector3i position;
    @Getter
    private final CloudChunk chunk;
    @Getter
    private final CloudLevel level;
    @Getter
    protected final CloudServer server;

    private final CloudBlockEntityData data;
    private final CloudPersistentDataContainer persistentData = new CloudPersistentDataContainer(this::setDirty);
    private NbtMap loadedTag = NbtMap.EMPTY;

    @Getter
    private boolean closed;
    private boolean pendingInitialUpdate = true;

    protected BaseBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        this(type, chunk, position, new CloudBlockEntityData());
    }

    protected BaseBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position, CloudBlockEntityData state) {
        this.type = checkNotNull(type, "type");
        this.position = checkNotNull(position, "position");
        this.data = checkNotNull(state, "state");

        Chunk owner = checkNotNull(chunk, "chunk");
        this.chunk = owner instanceof CloudChunk cloudChunk ? cloudChunk : null;
        this.level = owner.getLevel() instanceof CloudLevel cloudLevel ? cloudLevel : null;
        this.server = owner.getLevel().getServer() instanceof CloudServer cloudServer ? cloudServer : null;

        if (this.chunk != null) {
            this.chunk.registerBlockEntity(this);
        }

        if (this.level != null) {
            this.level.registerBlockEntity(this);
        }

        this.scheduleUpdate();
    }

    @Override
    public Block getBlock() {
        return this.level.getBlock(this.position);
    }

    @Override
    public BlockState getBlockState() {
        return this.level.getBlockState(this.position);
    }

    @Override
    public final boolean isValid() {
        return CloudBlockEntityRegistry.get().isValid(this.type, this.getBlockState());
    }

    @Override
    public boolean isMovable() {
        return this.data.isMovable();
    }

    @Override
    public void setMovable(boolean movable) {
        if (this.data.isMovable() != movable) {
            this.data.setMovable(movable);
            this.setDirty();
        }
    }

    @Override
    public @Nullable String getCustomName() {
        return this.data.getCustomName();
    }

    @Override
    public void setCustomName(@Nullable String customName) {
        if (!Objects.equals(this.data.getCustomName(), customName)) {
            this.data.setCustomName(customName);
            this.setDirty();
        }
    }

    @Override
    public boolean hasCustomName() {
        return this.getCustomName() != null;
    }

    @Override
    public PersistentDataContainer getPersistentDataContainer() {
        return this.persistentData;
    }

    protected CloudBlockEntityData data() {
        return this.data;
    }

    @Override
    public boolean onUpdate() {
        if (this.pendingInitialUpdate) {
            this.pendingInitialUpdate = false;
            if (this.isSpawnable()) {
                this.spawnToAll();
            }
        }

        return false;
    }

    @Override
    public final void scheduleUpdate() {
        if (!this.closed && this.level != null) {
            this.level.scheduleBlockEntityUpdate(this);
        }
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        if (this.chunk != null) {
            this.chunk.unregisterBlockEntity(this);
        }
        if (this.level != null) {
            this.level.unregisterBlockEntity(this);
        }
    }

    public void applyPlacementItem(ItemStack item) {
    }

    @Override
    public void onBreak() {
    }

    protected void dropItemOnBreak(@Nullable ItemStack item) {
        if (item != null && !item.isEmpty()) {
            this.level.dropBlockItem(this.position, item);
        }
    }

    @Override
    public void setDirty() {
        if (this.chunk != null) {
            this.chunk.markDirty();
        }
    }

    @Override
    public boolean isSpawnable() {
        return false;
    }

    @Override
    public void spawnTo(Player player) {
        if (!this.closed) {
            ((CloudPlayer) player).sendPacket(this.createUpdatePacket());
        }
    }

    @Override
    public void spawnToAll() {
        if (this.closed || this.chunk == null) {
            return;
        }

        for (CloudPlayer player : this.chunk.getViewers()) {
            if (player.spawned) {
                this.spawnTo(player);
            }
        }
    }

    public final void loadAdditionalData(NbtMap tag) {
        this.loadedTag = checkNotNull(tag, "tag");
        this.persistentData.load(tag);
        ServerNbt.MAPPER.readInto(tag, this.data);
        this.loadCustom(tag);
    }

    public final NbtMap getServerTag() {
        NbtMapBuilder tag = this.newTypedTag();
        this.writePosition(tag);
        this.writePersistentData(tag);
        return tag.build();
    }

    public final NbtMap getClientTag() {
        NbtMapBuilder tag = this.newTypedTag();
        this.writeClientData(tag);
        return tag.build();
    }

    public final NbtMap getChunkTag() {
        NbtMapBuilder tag = this.newTypedTag();
        this.writePosition(tag);
        this.writeClientData(tag);
        return tag.build();
    }

    protected void loadCustom(NbtMap tag) {
    }

    protected void saveCustom(NbtMapBuilder tag) {
    }

    protected void saveCustomClient(NbtMapBuilder tag) {
    }

    protected Set<String> customKeys() {
        return Set.of();
    }

    public final boolean updateFromClient(NbtMap tag, CloudPlayer player) {
        return !this.closed
                && CloudBlockEntityRegistry.get().getPersistentId(this.type).equals(tag.getString("id"))
                && this.applyClientUpdate(tag, player);
    }

    protected boolean applyClientUpdate(NbtMap tag, CloudPlayer player) {
        return false;
    }

    private NbtMapBuilder newTypedTag() {
        String id = CloudBlockEntityRegistry.get().getPersistentId(this.type);
        return NbtMap.builder().putString("id", id);
    }

    private void writePosition(NbtMapBuilder tag) {
        tag.putInt("x", this.position.getX());
        tag.putInt("y", this.position.getY());
        tag.putInt("z", this.position.getZ());
    }

    private void writePersistentData(NbtMapBuilder tag) {
        if (!this.loadedTag.isEmpty()) {
            Set<String> owned = ServerNbt.MAPPER.ownedKeys(this.data.getClass());
            Set<String> custom = this.customKeys();

            for (Map.Entry<String, Object> entry : this.loadedTag.entrySet()) {
                String key = entry.getKey();
                if (!SAVE_METADATA.contains(key) && !owned.contains(key) && !custom.contains(key)) {
                    tag.put(key, entry.getValue());
                }
            }
        }

        tag.putAll(ServerNbt.MAPPER.write(this.data));
        this.saveCustom(tag);
        this.persistentData.save(tag);
    }

    private void writeClientData(NbtMapBuilder tag) {
        tag.putAll(ClientNbtWriter.write(ServerNbt.MAPPER, this.data));
        this.saveCustomClient(tag);
    }

    private BlockEntityDataPacket createUpdatePacket() {
        BlockEntityDataPacket packet = new BlockEntityDataPacket();
        packet.setBlockPosition(this.position);
        packet.setData(this.getClientTag());
        return packet;
    }
}
