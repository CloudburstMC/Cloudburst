package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntitySnapshot;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.Objects;
import java.util.Set;

public record CloudEntitySnapshot(EntityType<?> type, NbtMap data) implements EntitySnapshot {

    private static final Set<String> PLACEMENT_DATA = Set.of(
            "identifier", "id", "UniqueID", "RuntimeID", "UUID", "UUIDMost", "UUIDLeast",
            "Pos", "Motion", "Rotation", "FallDistance", "OnGround", "Dimension",
            "TileX", "TileY", "TileZ", "Direction", "BeamTarget",
            "OwnerID", "OwnerNew", "Owner", "TargetID", "LeasherID",
            "LinksTag", "Passengers", "Riding", "RootVehicle", "PortalCooldown"
    );

    public CloudEntitySnapshot {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(data, "data");

        if (type == EntityTypes.PLAYER || Player.class.isAssignableFrom(type.getEntityClass())) {
            throw new IllegalArgumentException("Player state cannot be stored in an entity snapshot");
        }

        NbtMapBuilder persistentData = copyCompound(data).toBuilder();
        PLACEMENT_DATA.forEach(persistentData::remove);
        data = persistentData.build();
    }

    public static CloudEntitySnapshot capture(CloudEntity entity) {
        NbtMapBuilder data = NbtMap.builder();
        entity.saveAdditionalData(data);
        return new CloudEntitySnapshot(entity.getType(), data.build());
    }

    public static CloudEntity createFromItem(ItemStack item, EntityType<?> type, Location location) {
        EntitySnapshot snapshot = item.get(ItemDataComponents.ENTITY_DATA);
        if (snapshot == null) {
            return (CloudEntity) CloudEntityRegistry.get().create(type, location);
        }

        if (snapshot.getType() != type || !(snapshot instanceof CloudEntitySnapshot stored)) {
            throw new IllegalArgumentException("Item entity data does not match its placement type");
        }

        return stored.createEntity(location);
    }

    @Override
    public EntityType<?> getType() {
        return this.type;
    }

    @Override
    public NbtMap data() {
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
        restored.putAll(this.data());
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
