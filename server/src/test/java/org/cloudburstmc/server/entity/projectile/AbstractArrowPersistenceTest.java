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
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for the abstract-arrow branch. {@link EntityAbstractArrow} is abstract
 * and has no registered type mapping to its data, so the tests drive the production
 * load and save paths through a minimal subclass and assert the mapped NBT types.
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

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        TestArrow arrow = arrow();

        arrow.loadAdditionalData(NbtMap.builder()
                .putFloat("damage", 2.0f)
                .putBoolean("inGround", true)
                .putByte("PierceLevel", (byte) 5)
                .putInt("inBlockX", 1)
                .putInt("inBlockY", 2)
                .putInt("inBlockZ", 3)
                .putByte("pickup", (byte) 0)
                .build());

        assertEquals(ArrowPickupStatus.DISALLOWED, arrow.getPickupStatus());

        NbtMap written = save(arrow);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("damage", "inGround", "PierceLevel", "inBlockX", "inBlockY", "inBlockZ", "pickup",
                "PotionGroundTicks", "identifier"));
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

        NbtMap written = save(arrow);

        // Today's save always writes the flying state, no pierce or block, and open pickup.
        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("damage", "inGround", "PierceLevel", "pickup", "PotionGroundTicks", "identifier"));
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

        arrow.loadAdditionalData(NbtMap.builder()
                .putBoolean("inGround", true)
                .build());
        // The pickup rule has no presence bit: it always falls back to open pickup.
        assertEquals(ArrowPickupStatus.ALLOWED, arrow.getPickupStatus());

        // A tag without keys keeps the current values, as today's load did.
        arrow.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(3, arrow.getPierceLevel());
    }

    @Test
    void clearedBlockPositionIsNotWritten() throws Exception {
        TestArrow arrow = arrow();
        arrow.loadAdditionalData(NbtMap.builder()
                .putBoolean("inGround", true)
                .putInt("inBlockX", 1)
                .putInt("inBlockY", 2)
                .putInt("inBlockZ", 3)
                .build());

        // Pulling the arrow out clears the stuck state, so the block position is not written.
        arrow.loadAdditionalData(NbtMap.builder()
                .putBoolean("inGround", false)
                .build());

        NbtMap saved = save(arrow);
        assertFalse(saved.containsKey("inBlockX"), "saved tag still has inBlockX");
        assertFalse(saved.containsKey("inBlockY"), "saved tag still has inBlockY");
        assertFalse(saved.containsKey("inBlockZ"), "saved tag still has inBlockZ");
    }

    @Test
    void abstractArrowKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityAbstractArrow.class, AbstractArrowData.class);
        Set<String> fields = new HashSet<>();
        for (Field field : EntityAbstractArrow.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("pickupStatus", "pierceLevel", "inGround", "embeddedPosition")) {
            assertFalse(fields.contains(removed),
                    "EntityAbstractArrow keeps " + removed + "; state must live in AbstractArrowData only");
        }
    }
}
