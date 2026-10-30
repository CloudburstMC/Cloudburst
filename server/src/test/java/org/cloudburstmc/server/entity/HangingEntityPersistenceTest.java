package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.HangingData;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.Direction;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for hanging entities. {@link HangingEntity} is abstract and has no
 * registered type mapping to {@link HangingData}, so the tests drive the production
 * load and save paths through a minimal subclass and assert the mapped NBT types.
 * Tile coordinates are computed from the position on every save, so they travel
 * through the custom hooks.
 */
class HangingEntityPersistenceTest {

    private static final class TestHanging extends HangingEntity {
        TestHanging(EntityType<?> type, Location location) {
            super(type, location);
        }
    }

    private static TestHanging hanging() {
        PersistenceFixtures.ensureInitialized();
        TestHanging hanging = new TestHanging(EntityTypes.PAINTING,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        hanging.setPosition(Vector3f.from(1.2f, 64.7f, -3.4f));
        return hanging;
    }

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        TestHanging hanging = hanging();

        hanging.loadAdditionalData(NbtMap.builder()
                .putByte("Direction", (byte) 2)
                .build());

        assertEquals(2, hanging.getDirection().getIndex());

        NbtMap written = save(hanging);

        assertTrue(written.get("Direction") instanceof Byte, "Direction must be a byte");
        assertEquals((byte) 2, written.getByte("Direction"));

        NbtMapBuilder tiles = NbtMap.builder();
        hanging.saveCustom(tiles);
        NbtMap savedTiles = tiles.build();

        assertEquals(
                Set.of("TileX", "TileY", "TileZ", "Pos", "Motion", "Rotation", "FallDistance", "Fire", "OnGround", "Air",
                        "Scale", "CustomName", "CustomNameVisible", "CustomNameAlwaysVisible", "ActiveEffects"),
                hanging.customKeys());
        assertEquals(
                Set.of("TileX", "TileY", "TileZ", "Pos", "Motion", "Rotation", "FallDistance", "Fire", "OnGround", "Air",
                        "Scale"),
                savedTiles.keySet(), "saveCustom must write every present key customKeys() owns");
        assertTrue(savedTiles.get("TileX") instanceof Integer, "TileX must be an int");
        assertEquals(1, savedTiles.getInt("TileX"));
        assertEquals(64, savedTiles.getInt("TileY"));
        assertEquals(-3, savedTiles.getInt("TileZ"));
    }

    @Test
    void defaultInstanceFillsTodayDefaults() {
        TestHanging hanging = hanging();

        NbtMap written = save(hanging);

        // Today's save always writes the direction byte, zero for a fresh hanging.
        assertTrue(written.get("Direction") instanceof Byte, "Direction must be a byte");
        assertEquals((byte) 0, written.getByte("Direction"));
    }

    @Test
    void absentKeyLeavesStateUnchanged() {
        TestHanging hanging = hanging();
        hanging.data().setDirection(Direction.fromIndex(3));

        // A tag without keys keeps the current values, as today's load did.
        hanging.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(3, hanging.getDirection().getIndex());
    }

    @Test
    void hangingKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                HangingEntity.class, HangingData.class);
        Set<String> fields = new HashSet<>();
        for (Field field : HangingEntity.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("direction")) {
            assertFalse(fields.contains(removed),
                    "HangingEntity keeps " + removed + "; state must live in HangingData only");
        }
    }
}
