package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.AbstractArrowData;
import org.cloudburstmc.api.entity.projectile.AbstractArrow;
import org.cloudburstmc.api.entity.projectile.ArrowPickupStatus;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for the abstract-arrow branch. {@link EntityAbstractArrow} is abstract
 * and has no registered type mapping to {@link AbstractArrowData}, so the tests drive
 * {@code applyData}/{@code fillData} directly through a minimal subclass and assert the
 * mapped NBT types, exactly as the production save path would write them.
 */
class AbstractArrowPersistenceTest {

    private static final class TestArrow extends EntityAbstractArrow {
        TestArrow(EntityType<? extends AbstractArrow> type, Location location) {
            super(type, location);
        }
    }

    private static final Set<String> BASE_KEYS = Set.of(
            "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air",
            "OnGround", "Invulnerable", "Scale", "Tags");

    private static TestArrow arrow() {
        PersistenceFixtures.ensureInitialized();
        return new TestArrow(EntityTypes.ARROW,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
    }

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        TestArrow arrow = arrow();

        AbstractArrowData loaded = new AbstractArrowData();
        loaded.setDamage(2.0f);
        loaded.setInGround(true);
        loaded.setPierceLevel(5);
        loaded.setInBlockX(1);
        loaded.setInBlockY(2);
        loaded.setInBlockZ(3);
        loaded.setPickup(0);
        arrow.applyData(loaded);

        assertEquals(ArrowPickupStatus.DISALLOWED, arrow.getPickupStatus());

        AbstractArrowData filled = new AbstractArrowData();
        arrow.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("damage", "inGround", "PierceLevel", "inBlockX", "inBlockY", "inBlockZ", "pickup"));
        assertEquals(expectedKeys, written.keySet());
        assertEquals(NbtType.BYTE, NbtType.byClass(written.get("inGround").getClass()), "inGround must be a byte");
        assertTrue(written.getBoolean("inGround"));
        assertTrue(written.get("PierceLevel") instanceof Byte, "PierceLevel must be a byte");
        assertEquals((byte) 5, written.getByte("PierceLevel"));
        assertTrue(written.get("inBlockX") instanceof Integer, "inBlockX must be an int");
        assertEquals(1, written.getInt("inBlockX"));
        assertEquals(2, written.getInt("inBlockY"));
        assertEquals(3, written.getInt("inBlockZ"));
        assertTrue(written.get("pickup") instanceof Byte, "pickup must be a byte");
        assertEquals((byte) 0, written.getByte("pickup"));
    }

    @Test
    void defaultInstanceFillsTodayDefaults() {
        TestArrow arrow = arrow();

        AbstractArrowData filled = new AbstractArrowData();
        arrow.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

        // Today's save always writes the flying state, no pierce or block, and open pickup.
        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("damage", "inGround", "PierceLevel", "pickup"));
        assertEquals(expectedKeys, written.keySet());
        assertEquals(NbtType.BYTE, NbtType.byClass(written.get("inGround").getClass()), "inGround must be a byte");
        assertFalse(written.getBoolean("inGround"));
        assertEquals((byte) 0, written.getByte("PierceLevel"));
        assertEquals((byte) 1, written.getByte("pickup"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        TestArrow arrow = arrow();
        arrow.setPierceLevel(3);
        arrow.setPickupStatus(ArrowPickupStatus.CREATIVE_ONLY);

        AbstractArrowData loaded = new AbstractArrowData();
        loaded.setInGround(true);
        arrow.applyData(loaded);
        // The pickup rule has no presence bit: it always falls back to open pickup.
        assertEquals(ArrowPickupStatus.ALLOWED, arrow.getPickupStatus());

        // All DTO fields null, as the mapper leaves them when the keys are absent.
        arrow.applyData(new AbstractArrowData());

        assertEquals(3, arrow.getPierceLevel());
    }
}
