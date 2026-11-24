package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.passive.Tameable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TameablePersistenceTest {

    private static final UUID OWNER_ID = UUID.fromString("12345678-1234-4321-8234-123456789abc");

    @Test
    void registeredTameablesRoundTripOfflineOwnershipAndSitting() {
        for (EntityType<? extends Tameable> type : List.of(EntityTypes.CAT, EntityTypes.WOLF, EntityTypes.PARROT)) {
            EntityTameable original = create(type);
            original.setOwnerUniqueId(OWNER_ID);
            original.setSitting(true);

            NbtMap saved = save(original);
            assertEquals(OWNER_ID.toString(), saved.getString("Cloudburst.OwnerUUID"));
            assertFalse(saved.containsKey("OwnerID"));
            assertInstanceOf(Byte.class, saved.get("Sitting"));

            EntityTameable reloaded = create(type);
            reloaded.loadAdditionalData(saved);
            assertEquals(OWNER_ID, reloaded.getOwnerUniqueId());
            assertNull(reloaded.getOwner());
            assertTrue(reloaded.isTamed());
            assertTrue(reloaded.isSitting());

            reloaded.setTamed(false);
            assertNull(reloaded.getOwnerUniqueId());
            assertFalse(reloaded.isTamed());
            assertFalse(save(reloaded).containsKey("Cloudburst.OwnerUUID"));
        }
    }

    @Test
    void ownerlessTamedStateRoundTripsInTheNewFormat() {
        EntityTameable tameable = create(EntityTypes.WOLF);
        tameable.setTamed(true);

        NbtMap saved = save(tameable);
        assertTrue(saved.getBoolean("Cloudburst.Tamed"));
        assertFalse(saved.containsKey("Cloudburst.OwnerUUID"));
        EntityTameable reloaded = create(EntityTypes.WOLF);
        reloaded.loadAdditionalData(saved);
        assertNull(reloaded.getOwnerUniqueId());
        assertTrue(reloaded.isTamed());
    }

    @Test
    void loadingUntamedDataClearsThePriorOwner() {
        EntityTameable tameable = create(EntityTypes.WOLF);
        tameable.setOwnerUniqueId(OWNER_ID);
        tameable.setSitting(true);

        tameable.loadAdditionalData(NbtMap.EMPTY);

        assertNull(tameable.getOwnerUniqueId());
        assertFalse(tameable.isTamed());
        assertFalse(tameable.isSitting());
    }

    @Test
    void invalidOwnerUuidDoesNotTameTheEntity() {
        EntityTameable tameable = create(EntityTypes.WOLF);

        tameable.loadAdditionalData(NbtMap.builder().putString("Cloudburst.OwnerUUID", "invalid").build());

        assertNull(tameable.getOwnerUniqueId());
        assertFalse(tameable.isTamed());
    }

    private static EntityTameable create(EntityType<? extends Tameable> type) {
        return assertInstanceOf(EntityTameable.class, PersistenceFixtures.entity(type));
    }

    private static NbtMap save(EntityTameable tameable) {
        NbtMapBuilder tag = NbtMap.builder();
        tameable.saveAdditionalData(tag);
        return tag.build();
    }
}
