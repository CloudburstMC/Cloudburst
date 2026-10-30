package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.HangingData;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for hanging entities. {@link HangingEntity} is abstract and has no
 * registered type mapping to {@link HangingData}, so the tests drive
 * {@code applyData}/{@code fillData} directly and assert the mapped NBT types,
 * exactly as the production save path would write them. Tile coordinates are
 * computed from the position on every save, so they travel through the custom hooks.
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

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        TestHanging hanging = hanging();

        HangingData loaded = new HangingData();
        loaded.setDirection(2);
        hanging.applyData(loaded);

        assertEquals(2, hanging.direction);

        // Base data carries a default origin, so restore the position after applying.
        hanging.setPosition(Vector3f.from(1.2f, 64.7f, -3.4f));

        HangingData filled = new HangingData();
        hanging.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

        assertTrue(written.get("Direction") instanceof Byte, "Direction must be a byte");
        assertEquals((byte) 2, written.getByte("Direction"));

        NbtMapBuilder tiles = NbtMap.builder();
        hanging.saveCustom(tiles);
        NbtMap savedTiles = tiles.build();

        assertEquals(Set.of("TileX", "TileY", "TileZ"), hanging.customKeys());
        assertEquals(Set.of("TileX", "TileY", "TileZ"), savedTiles.keySet());
        assertTrue(savedTiles.get("TileX") instanceof Integer, "TileX must be an int");
        assertEquals(1, savedTiles.getInt("TileX"));
        assertEquals(64, savedTiles.getInt("TileY"));
        assertEquals(-3, savedTiles.getInt("TileZ"));
    }

    @Test
    void defaultInstanceFillsTodayDefaults() {
        TestHanging hanging = hanging();

        HangingData filled = new HangingData();
        hanging.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

        // Today's save always writes the direction byte, zero for a fresh hanging.
        assertTrue(written.get("Direction") instanceof Byte, "Direction must be a byte");
        assertEquals((byte) 0, written.getByte("Direction"));
    }

    @Test
    void absentKeyLeavesStateUnchanged() {
        TestHanging hanging = hanging();
        hanging.direction = 3;

        // All DTO fields null, as the mapper leaves them when the keys are absent.
        hanging.applyData(new HangingData());

        assertEquals(3, hanging.direction);
    }
}
