package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.TntMinecartData;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the TNT-minecart data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class TntMinecartPersistenceTest {

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
        assertEquals(TntMinecartData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.TNT_MINECART));

        EntityTntMinecart minecart = (EntityTntMinecart) PersistenceFixtures.entity(EntityTypes.TNT_MINECART);
        NbtMap input = NbtMap.builder()
                .putInt("Fuse", 40)
                .putInt("DisplayOffset", 9)
                .putInt("Mystery", 7)
                .build();
        minecart.loadAdditionalData(input);

        assertEquals(40, minecart.getFuse());
        assertEquals(9, minecart.getDisplayOffset());

        NbtMap saved = save(minecart);

        // A TNT cart always carries its TNT display block.
        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(BASE_KEYS);
        expectedKeys.addAll(Set.of("CustomDisplayTile", "DisplayBlock", "identifier"));
        assertEquals(expectedKeys, saved.keySet());

        assertTrue(saved.getBoolean("CustomDisplayTile"));
        assertEquals(BlockStateCodec.write(BlockStates.TNT), saved.getCompound("DisplayBlock"));

        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityTntMinecart minecart = (EntityTntMinecart) PersistenceFixtures.entity(EntityTypes.TNT_MINECART);

        NbtMap saved = save(minecart);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("Fuse", "CustomDisplayTile", "DisplayBlock", "identifier"));
        assertEquals(expectedKeys, saved.keySet());

        // Today's save always writes the unprimed fuse and the TNT display block,
        // and omits the default offset.
        assertTrue(saved.get("Fuse") instanceof Integer, "Fuse must be an int");
        assertEquals(-1, saved.getInt("Fuse"));
        assertTrue(saved.getBoolean("CustomDisplayTile"));
        assertEquals(BlockStateCodec.write(BlockStates.TNT), saved.getCompound("DisplayBlock"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        EntityTntMinecart minecart = (EntityTntMinecart) PersistenceFixtures.entity(EntityTypes.TNT_MINECART);
        minecart.setFuse(30);
        minecart.setDisplayBlockOffset(9);

        minecart.loadAdditionalData(NbtMap.EMPTY);

        // The fuse has a presence bit; the offset always falls back to its default.
        assertEquals(30, minecart.getFuse());
        assertEquals(6, minecart.getDisplayOffset());
    }

    @Test
    void tntMinecartKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityTntMinecart.class, TntMinecartData.class);
        Set<String> fields = new HashSet<>();
        for (Field field : EntityTntMinecart.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("fuse")) {
            assertFalse(fields.contains(removed),
                    "EntityTntMinecart keeps " + removed + "; state must live in TntMinecartData only");
        }
    }
}
