package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.PaintingData;
import org.cloudburstmc.api.entity.misc.Painting.Motive;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the painting data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types. Tile coordinates are
 * recomputed from the position on save, so the input values are replaced.
 */
class PaintingPersistenceTest {

    private static final Set<String> BASE_KEYS = Set.of(
            "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air",
            "OnGround", "Invulnerable", "Scale", "Tags");

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(PaintingData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.PAINTING));

        EntityPainting painting = (EntityPainting) PersistenceFixtures.entity(EntityTypes.PAINTING);
        NbtMap input = NbtMap.builder()
                .putByte("Direction", (byte) 1)
                .putInt("TileX", 9)
                .putInt("TileY", 9)
                .putInt("TileZ", 9)
                .putString("Motive", "Sunset")
                .putInt("Mystery", 7)
                .build();
        painting.loadAdditionalData(input);

        assertEquals(Motive.SUNSET, painting.getMotive());

        NbtMap saved = save(painting);

        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(BASE_KEYS);
        expectedKeys.add("identifier");
        assertEquals(expectedKeys, saved.keySet());

        assertTrue(saved.get("Direction") instanceof Byte, "Direction must be a byte");
        assertEquals((byte) 1, saved.getByte("Direction"));
        assertTrue(saved.get("Motive") instanceof String, "Motive must be a string");
        assertEquals("Sunset", saved.getString("Motive"));
        assertEquals(7, saved.getInt("Mystery"));
        // Tiles are recomputed from the stub position, which is the origin.
        assertTrue(saved.get("TileX") instanceof Integer, "TileX must be an int");
        assertEquals(0, saved.getInt("TileX"));
        assertEquals(0, saved.getInt("TileY"));
        assertEquals(0, saved.getInt("TileZ"));
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityPainting painting = (EntityPainting) PersistenceFixtures.entity(EntityTypes.PAINTING);

        NbtMap saved = save(painting);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("Direction", "TileX", "TileY", "TileZ", "Motive", "identifier"));
        assertEquals(expectedKeys, saved.keySet());

        // Today's save always writes direction zero, origin tiles and the Kebab motive.
        assertTrue(saved.get("Direction") instanceof Byte, "Direction must be a byte");
        assertEquals((byte) 0, saved.getByte("Direction"));
        assertEquals(0, saved.getInt("TileX"));
        assertEquals(0, saved.getInt("TileY"));
        assertEquals(0, saved.getInt("TileZ"));
        assertEquals("Kebab", saved.getString("Motive"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        EntityPainting painting = (EntityPainting) PersistenceFixtures.entity(EntityTypes.PAINTING);
        painting.loadAdditionalData(NbtMap.builder()
                .putByte("Direction", (byte) 3)
                .putString("Motive", "Sunset")
                .build());

        painting.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(3, painting.getDirection().getIndex());
        assertEquals(Motive.SUNSET, painting.getMotive());
    }
}
