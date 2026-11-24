package org.cloudburstmc.server.entity;

import lombok.EqualsAndHashCode;
import org.cloudburstmc.api.entity.EntitySnapshot;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.Objects;
import java.util.Set;

@EqualsAndHashCode(doNotUseGetters = true)
public class CloudEntitySnapshot implements EntitySnapshot {

    private static final Set<String> NON_TRANSFERABLE_KEYS = Set.of(
            "identifier", "id", "UniqueID", "RuntimeID", "UUID", "UUIDMost", "UUIDLeast",
            "Pos", "Motion", "Rotation", "FallDistance", "OnGround", "Dimension", "PortalCooldown",
            "TileX", "TileY", "TileZ", "Direction", "BeamTarget",
            "OwnerID", "OwnerNew", "Owner", "Cloudburst.OwnerUUID", "Cloudburst.Tamed",
            "TargetID", "LeasherID", "LinksTag", "Passengers", "Riding", "RootVehicle"
    );

    private final EntityType<?> type;
    private final NbtMap data;

    public CloudEntitySnapshot(EntityType<?> type, NbtMap data) {
        this.type = Objects.requireNonNull(type, "type");
        Objects.requireNonNull(data, "data");

        if (type == EntityTypes.PLAYER || Player.class.isAssignableFrom(type.getEntityClass())) {
            throw new IllegalArgumentException("Player state cannot be stored in an entity snapshot");
        }

        NbtMapBuilder persistentData = NbtMap.builder();
        data.forEach((key, value) -> {
            if (!NON_TRANSFERABLE_KEYS.contains(key)) {
                persistentData.put(key, copyValue(value));
            }
        });

        this.data = persistentData.build();
    }

    public static CloudEntitySnapshot capture(CloudEntity entity) {
        NbtMapBuilder data = NbtMap.builder();
        entity.saveAdditionalData(data);
        return new CloudEntitySnapshot(entity.getType(), data.build());
    }

    @Override
    public EntityType<?> getType() {
        return this.type;
    }

    public NbtMap copyData() {
        return copyCompound(this.data);
    }

    @Override
    public CloudEntity createEntity(Location location) {
        CloudEntity entity = (CloudEntity) CloudEntityRegistry.get().create(this.type, location);
        if (entity.getType() != this.type || entity.isClosed()) {
            throw new IllegalArgumentException("Snapshot factory must create an entity of the captured type");
        }

        NbtMapBuilder restored = NbtMap.builder();
        entity.saveAdditionalData(restored);
        restored.putAll(this.copyData());
        entity.loadAdditionalData(restored.build());
        return entity;
    }

    private static NbtMap copyCompound(NbtMap data) {
        NbtMapBuilder copy = NbtMap.builder();
        data.forEach((key, value) -> copy.put(key, copyValue(value)));
        return copy.build();
    }

    private static Object copyValue(Object value) {
        return switch (value) {
            case NbtMap compound -> copyCompound(compound);
            case NbtList<?> list -> copyList(list);
            default -> NbtUtils.copy(value);
        };
    }

    private static <T> NbtList<T> copyList(NbtList<T> list) {
        return new NbtList<>(list.getType(), list.stream()
                .map(value -> list.getType().getTagClass().cast(copyValue(value)))
                .toList());
    }
}
