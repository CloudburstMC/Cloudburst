package org.cloudburstmc.server.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.EndGateway;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.EndGatewayData;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.packet.BlockEventPacket;

import java.util.List;
import java.util.Set;

public class EndGatewayBlockEntity extends BaseBlockEntity implements EndGateway {

    private static final int COOLDOWN_TICKS = 40;
    private static final int ATTENTION_INTERVAL = 2400;

    private int teleportCooldown;

    public EndGatewayBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return EndGatewayData.class;
    }

    @Override
    protected EndGatewayData data() {
        return (EndGatewayData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        tag.listenForList("ExitPortal", NbtType.INT, coordinates -> {
            if (coordinates.size() >= 3) {
                data().setExitPortal(Vector3i.from(coordinates.get(0), coordinates.get(1), coordinates.get(2)));
            }
        });
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        Vector3i exit = data().getExitPortal() == null ? Vector3i.ZERO : data().getExitPortal();
        tag.putList("ExitPortal", NbtType.INT, List.of(exit.getX(), exit.getY(), exit.getZ()));
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        tag.putInt("Age", networkAge());
        Vector3i exit = data().getExitPortal() == null ? Vector3i.ZERO : data().getExitPortal();
        tag.putList("ExitPortal", NbtType.INT, List.of(exit.getX(), exit.getY(), exit.getZ()));
    }

    @Override
    protected Set<String> customKeys() {
        return Set.of("ExitPortal");
    }

    @Override
    public boolean onUpdate() {
        super.onUpdate();

        data().setAge(data().getAge() + 1);
        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        } else if (data().getAge() % ATTENTION_INTERVAL == 0) {
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
        return data().getExitPortal() == null ? null : Location.from(data().getExitPortal(), this.getLevel());
    }

    @Override
    public void setExitLocation(@Nullable Location location) {
        if (location != null && location.getLevel() != this.getLevel()) {
            throw new IllegalArgumentException("End gateway exit must be in the gateway's level");
        }

        data().setExitPortal(location == null ? null : location.getPosition().toInt());
        this.setDirty();
    }

    @Override
    public boolean isExactTeleport() {
        return data().isExactTeleport();
    }

    @Override
    public void setExactTeleport(boolean exactTeleport) {
        data().setExactTeleport(exactTeleport);
        this.setDirty();
    }

    @Override
    public long getAge() {
        return data().getAge();
    }

    @Override
    public void setAge(long age) {
        data().setAge(age);
        this.setDirty();
    }

    private int networkAge() {
        long age = data().getAge();
        if (age < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }

        if (age > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) age;
    }
}
