package org.cloudburstmc.server.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.EndGateway;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.packet.BlockEventPacket;

import java.util.List;

public class EndGatewayBlockEntity extends BaseBlockEntity implements EndGateway {

    private static final int COOLDOWN_TICKS = 40;
    private static final int ATTENTION_INTERVAL = 2400;

    private long age;
    private int teleportCooldown;
    private @Nullable Vector3i exitPosition;
    private boolean exactTeleport;

    public EndGatewayBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        tag.listenForLong("Age", this::setAge);
        tag.listenForBoolean("ExactTeleport", this::setExactTeleport);
        tag.listenForList("ExitPortal", NbtType.INT, coordinates -> {
            if (coordinates.size() >= 3) {
                this.exitPosition = Vector3i.from(coordinates.get(0), coordinates.get(1), coordinates.get(2));
            }
        });
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.putLong("Age", this.age);
        tag.putBoolean("ExactTeleport", this.exactTeleport);
        if (this.exitPosition != null) {
            tag.putList("ExitPortal", NbtType.INT, List.of(this.exitPosition.getX(), this.exitPosition.getY(), this.exitPosition.getZ()));
        }
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        tag.putInt("Age", networkAge());
        Vector3i exit = this.exitPosition == null ? Vector3i.ZERO : this.exitPosition;
        tag.putList("ExitPortal", NbtType.INT, List.of(exit.getX(), exit.getY(), exit.getZ()));
    }

    @Override
    public boolean onUpdate() {
        super.onUpdate();

        this.age++;
        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        } else if (this.age % ATTENTION_INTERVAL == 0) {
            this.startCooldown();
        }

        return true;
    }

    public boolean isCoolingDown() {
        return this.teleportCooldown > 0;
    }

    public void startCooldown() {
        this.teleportCooldown = COOLDOWN_TICKS;
        this.setDirty();

        BlockEventPacket packet = new BlockEventPacket();
        packet.setBlockPosition(this.getPosition());
        packet.setEventType(1);
        packet.setEventData(0);
        this.getLevel().addChunkPacket(this.getPosition(), packet);
    }

    @Override
    public @Nullable Location getExitLocation() {
        return this.exitPosition == null ? null : Location.from(this.exitPosition, this.getLevel());
    }

    @Override
    public void setExitLocation(@Nullable Location location) {
        if (location != null && location.getLevel() != this.getLevel()) {
            throw new IllegalArgumentException("End gateway exit must be in the gateway's level");
        }

        this.exitPosition = location == null ? null : location.getPosition().toInt();
        this.setDirty();
    }

    @Override
    public boolean isExactTeleport() {
        return this.exactTeleport;
    }

    @Override
    public void setExactTeleport(boolean exactTeleport) {
        this.exactTeleport = exactTeleport;
        this.setDirty();
    }

    @Override
    public long getAge() {
        return this.age;
    }

    @Override
    public void setAge(long age) {
        this.age = age;
        this.setDirty();
    }

    private int networkAge() {
        if (this.age < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }

        if (this.age > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) this.age;
    }
}
