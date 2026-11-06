package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.CreatureData;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for the tameable branch. {@link EntityTameable} is abstract and has no
 * registered subclasses, so the tests drive the production load/save hooks directly
 * through a minimal subclass and assert the written NBT types, exactly as the
 * production save path would write them.
 */
class TameablePersistenceTest {

    private static final class TestTameable extends EntityTameable {
        TestTameable(EntityType<?> type, Location location) {
            super(type, location);
        }
    }

    private static TestTameable tameable() {
        PersistenceFixtures.ensureInitialized();
        return new TestTameable(EntityTypes.WOLF,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
    }

    private static NbtMap save(TestTameable tameable) {
        NbtMapBuilder builder = NbtMap.builder();
        tameable.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        TestTameable tameable = tameable();

        tameable.loadAdditionalData(NbtMap.builder()
                .putLong("OwnerID", 123L)
                .putBoolean("Sitting", true)
                .build());

        assertEquals(123L, tameable.getOwnerId());
        assertTrue(tameable.isTamed());
        assertTrue(tameable.isSitting());

        NbtMap written = save(tameable);

        assertTrue(written.get("OwnerID") instanceof Long, "OwnerID must be a long");
        assertEquals(123L, written.getLong("OwnerID"));
        assertEquals(NbtType.BYTE, NbtType.byClass(written.get("Sitting").getClass()), "Sitting must be a byte");
        assertTrue(written.getBoolean("Sitting"));
    }

    @Test
    void defaultInstanceFillsTodayDefaults() {
        TestTameable tameable = tameable();

        NbtMap written = save(tameable);

        // Today's save always writes the unset owner id (-1) and Sitting false.
        assertTrue(written.get("OwnerID") instanceof Long, "OwnerID must be a long");
        assertEquals(-1L, written.getLong("OwnerID"));
        assertEquals(NbtType.BYTE, NbtType.byClass(written.get("Sitting").getClass()), "Sitting must be a byte");
        assertFalse(written.getBoolean("Sitting"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        TestTameable tameable = tameable();
        tameable.setOwnerId(7L);
        tameable.setTamed(true);
        tameable.setSitting(true);

        tameable.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(7L, tameable.getOwnerId());
        assertTrue(tameable.isTamed());
        assertTrue(tameable.isSitting());
    }

    @Test
    void clearedOwnerGivesTodayOutput() {
        TestTameable tameable = tameable();
        tameable.loadAdditionalData(NbtMap.builder()
                .putLong("OwnerID", 123L)
                .putBoolean("Sitting", true)
                .build());

        tameable.setOwner(null);

        NbtMap written = save(tameable);

        assertTrue(written.get("OwnerID") instanceof Long, "OwnerID must be a long");
        assertEquals(-1L, written.getLong("OwnerID"));
        assertEquals(NbtType.BYTE, NbtType.byClass(written.get("Sitting").getClass()), "Sitting must be a byte");
        assertTrue(written.getBoolean("Sitting"));
    }

    @Test
    void nonNumericOwnerIdLeavesStateUnchanged() {
        TestTameable tameable = tameable();
        tameable.setOwnerId(7L);

        // Today's load ignored a non-numeric OwnerID: the mapper bound it to
        // null, so nothing was tamed.
        tameable.loadAdditionalData(NbtMap.builder()
                .putString("OwnerID", "not-a-number")
                .build());

        assertEquals(7L, tameable.getOwnerId());
        assertFalse(tameable.isTamed());
    }

    @Test
    void tameableKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityTameable.class, CreatureData.class,
                Map.of(
                        "armor", "an armor view over the live container in data().getArmor()",
                        "offhand", "an offhand view over the live container in data().getOffhand()"));
        Set<String> fields = new HashSet<>();
        for (Field field : EntityTameable.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("ownerId", "sitting")) {
            assertFalse(fields.contains(removed),
                    "EntityTameable keeps " + removed + "; state must live in metadata only");
        }
    }
}
