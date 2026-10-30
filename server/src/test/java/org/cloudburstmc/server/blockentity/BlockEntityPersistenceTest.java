package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.nbt.NbtMapper;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.cloudburstmc.server.testutil.TestBlockEntities.LegacyBlockEntity;
import org.cloudburstmc.server.testutil.TestBlockEntities.MigratedBlockEntity;
import org.junit.jupiter.api.Test;

import static org.cloudburstmc.server.testutil.TestBlockEntities.LEGACY_TYPE;
import static org.cloudburstmc.server.testutil.TestBlockEntities.MIGRATED_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the block-entity data path: unknown keys survive in the
 * raw tag, DTO properties write exactly, custom hooks own conditional keys,
 * plugin data stays server-side, and legacy registrations keep raw keys.
 */
class BlockEntityPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(1, 2, 3);

    @Test
    void unknownKeysSurviveSave() {
        MigratedBlockEntity entity = PersistenceFixtures.blockEntity(MIGRATED_TYPE, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "TestMigrated")
                .putInt("x", 1)
                .putInt("y", 2)
                .putInt("z", 3)
                .putByte("isMovable", (byte) 1)
                .putInt("Count", 3)
                .putInt("Mystery", 7)
                .build());

        NbtMap saved = entity.getServerTag();

        assertEquals(7, saved.getInt("Mystery"));
        assertEquals(3, saved.getInt("Count"));
        assertTrue(saved.getBoolean("isMovable"));

        assertEquals(3, entity.getClientTag().getInt("Count"));
        assertFalse(entity.getClientTag().containsKey("Mystery"));
        assertFalse(entity.getChunkTag().containsKey("Mystery"));
    }

    @Test
    void clearedCustomNameStaysCleared() {
        MigratedBlockEntity entity = PersistenceFixtures.blockEntity(MIGRATED_TYPE, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "TestMigrated")
                .putInt("x", 1)
                .putInt("y", 2)
                .putInt("z", 3)
                .putString("CustomName", "A")
                .build());

        assertEquals("A", entity.getCustomName());

        entity.setCustomName(null);

        assertFalse(entity.getServerTag().containsKey("CustomName"));
    }

    @Test
    void customKeysAreRemovedFromRawBeforeSave() {
        MigratedBlockEntity entity = PersistenceFixtures.blockEntity(MIGRATED_TYPE, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "TestMigrated")
                .putInt("x", 1)
                .putInt("y", 2)
                .putInt("z", 3)
                .putBoolean("Flag", true)
                .build());

        entity.setFlag(false);

        assertFalse(entity.getServerTag().containsKey("Flag"));

        entity.setFlag(true);

        assertTrue(entity.getServerTag().getBoolean("Flag"));
    }

    @Test
    void pluginDataSurvivesAndStaysServerSide() {
        MigratedBlockEntity entity = PersistenceFixtures.blockEntity(MIGRATED_TYPE, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "TestMigrated")
                .putInt("x", 1)
                .putInt("y", 2)
                .putInt("z", 3)
                .build());

        entity.setPluginData("myplugin", NbtMap.builder().putInt("a", 1).build());

        NbtMap saved = entity.getServerTag();
        assertEquals(1, saved.getCompound(NbtMapper.PLUGIN_DATA_KEY).getCompound("myplugin").getInt("a"));
        assertFalse(entity.getClientTag().containsKey(NbtMapper.PLUGIN_DATA_KEY));
        assertFalse(entity.getChunkTag().containsKey(NbtMapper.PLUGIN_DATA_KEY));

        MigratedBlockEntity reloaded = PersistenceFixtures.blockEntity(MIGRATED_TYPE, POSITION);
        reloaded.loadAdditionalData(saved);

        assertEquals(1, reloaded.getPluginData("myplugin").orElseThrow().getInt("a"));

        reloaded.removePluginData("myplugin");

        assertTrue(reloaded.getPluginData("myplugin").isEmpty());
        assertFalse(reloaded.getServerTag().containsKey(NbtMapper.PLUGIN_DATA_KEY));
    }

    @Test
    void legacyRegistrationUsesBaseDataAndKeepsRawKeys() {
        assertEquals(BlockEntityData.class,
                CloudBlockEntityRegistry.get().getDataClass(LEGACY_TYPE));

        LegacyBlockEntity entity = PersistenceFixtures.blockEntity(LEGACY_TYPE, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "TestLegacy")
                .putInt("x", 1)
                .putInt("y", 2)
                .putInt("z", 3)
                .putByte("isMovable", (byte) 0)
                .putString("Custom", "v")
                .build());

        NbtMap saved = entity.getServerTag();

        assertEquals("v", saved.getString("Custom"));
        assertFalse(saved.getBoolean("isMovable"));
    }
}
