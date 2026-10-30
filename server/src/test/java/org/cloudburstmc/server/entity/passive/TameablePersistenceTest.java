package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.TameableData;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for the tameable branch. {@link EntityTameable} is abstract and has no
 * registered subclasses, so no registry type maps to {@link TameableData}; the tests drive
 * {@code applyData}/{@code fillData} directly through a minimal subclass and assert the
 * mapped NBT types, exactly as the production save path would write them.
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

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        TestTameable tameable = tameable();

        TameableData loaded = new TameableData();
        loaded.setOwnerId(123L);
        loaded.setSitting(true);
        tameable.applyData(loaded);

        assertEquals(123L, tameable.getOwnerId());
        assertTrue(tameable.isTamed());
        assertTrue(tameable.isSitting());

        TameableData filled = new TameableData();
        tameable.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

        assertTrue(written.get("OwnerID") instanceof Long, "OwnerID must be a long");
        assertEquals(123L, written.getLong("OwnerID"));
        assertEquals(NbtType.BYTE, NbtType.byClass(written.get("Sitting").getClass()), "Sitting must be a byte");
        assertEquals(true, written.getBoolean("Sitting"));
    }

    @Test
    void defaultInstanceFillsTodayDefaults() {
        TestTameable tameable = tameable();

        TameableData filled = new TameableData();
        tameable.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

        // Today's save always writes the unset owner id (-1) and Sitting false.
        assertTrue(written.get("OwnerID") instanceof Long, "OwnerID must be a long");
        assertEquals(-1L, written.getLong("OwnerID"));
        assertEquals(NbtType.BYTE, NbtType.byClass(written.get("Sitting").getClass()), "Sitting must be a byte");
        assertEquals(false, written.getBoolean("Sitting"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        TestTameable tameable = tameable();
        tameable.setOwnerId(7L);
        tameable.setTamed(true);
        tameable.setSitting(true);

        // All DTO fields null, as the mapper leaves them when the keys are absent.
        tameable.applyData(new TameableData());

        assertEquals(7L, tameable.getOwnerId());
        assertTrue(tameable.isTamed());
        assertTrue(tameable.isSitting());
    }
}
