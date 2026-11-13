package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.persistence.PersistentDataType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.data.BlockEntityData;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.persistence.CloudPersistentDataContainer;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.cloudburstmc.server.testutil.TestBlockEntities.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trip tests for the block-entity data path: unknown keys survive in the
 * raw tag, DTO properties write exactly, custom hooks own conditional keys,
 * plugin data stays server-side, and legacy registrations keep raw keys.
 */
class BlockEntityPersistenceTest {

    private static final Identifier PLUGIN_KEY = Identifier.parse("myplugin:a");

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

        entity.getPersistentDataContainer().set(PLUGIN_KEY, PersistentDataType.INTEGER, 1);

        NbtMap saved = entity.getServerTag();
        assertEquals(1, saved.getCompound(CloudPersistentDataContainer.STORAGE_KEY).getInt("myplugin:a"));
        assertFalse(entity.getClientTag().containsKey(CloudPersistentDataContainer.STORAGE_KEY));
        assertFalse(entity.getChunkTag().containsKey(CloudPersistentDataContainer.STORAGE_KEY));

        MigratedBlockEntity reloaded = PersistenceFixtures.blockEntity(MIGRATED_TYPE, POSITION);
        reloaded.loadAdditionalData(saved);

        assertEquals(1, reloaded.getPersistentDataContainer().get(PLUGIN_KEY, PersistentDataType.INTEGER).orElseThrow().intValue());

        reloaded.getPersistentDataContainer().remove(PLUGIN_KEY);

        assertTrue(reloaded.getPersistentDataContainer().get(PLUGIN_KEY, PersistentDataType.INTEGER).isEmpty());
        assertFalse(reloaded.getServerTag().containsKey(CloudPersistentDataContainer.STORAGE_KEY));
    }

    @Test
    void pluginDataMarksChunkDirty() {
        MigratedBlockEntity entity = PersistenceFixtures.blockEntity(MIGRATED_TYPE, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "TestMigrated")
                .putInt("x", 1)
                .putInt("y", 2)
                .putInt("z", 3)
                .build());

        long revision = entity.getChunk().captureSaveRevision();
        entity.getPersistentDataContainer().set(PLUGIN_KEY, PersistentDataType.INTEGER, 1);
        assertNotEquals(revision, entity.getChunk().captureSaveRevision());

        revision = entity.getChunk().captureSaveRevision();
        entity.getPersistentDataContainer().remove(PLUGIN_KEY);
        assertNotEquals(revision, entity.getChunk().captureSaveRevision());
    }

    @Test
    void dataInstanceIsStable() {
        MigratedBlockEntity entity = PersistenceFixtures.blockEntity(MIGRATED_TYPE, POSITION);
        BlockEntityData before = entity.data();
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "TestMigrated")
                .putInt("x", 1)
                .putInt("y", 2)
                .putInt("z", 3)
                .putInt("Count", 3)
                .build());
        assertSame(before, entity.data());
    }

    @Test
    void absentKeyKeepsCurrentValue() {
        MigratedBlockEntity entity = PersistenceFixtures.blockEntity(MIGRATED_TYPE, POSITION);
        entity.setCustomName("A");
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "TestMigrated")
                .putInt("x", 1)
                .putInt("y", 2)
                .putInt("z", 3)
                .build());
        assertEquals("A", entity.getCustomName());
    }

    @Test
    void saveReadsFromData() {
        MigratedBlockEntity entity = PersistenceFixtures.blockEntity(MIGRATED_TYPE, POSITION);
        entity.setCustomName("B");
        NbtMap saved = entity.getServerTag();
        assertEquals("B", saved.getString("CustomName"));
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

    @Test
    void legacyChestRegistrationUsesChestData() {
        assertEquals(BlockEntityData.class,
                CloudBlockEntityRegistry.get().getDataClass(PLUGIN_CHEST_TYPE));

        PluginChestBlockEntity entity = PersistenceFixtures.blockEntity(PLUGIN_CHEST_TYPE, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "TestPluginChest")
                .putInt("x", 1)
                .putInt("y", 2)
                .putInt("z", 3)
                .putList("Items", NbtType.COMPOUND, List.of(
                        ItemStackNbtCodec.serializeItem(ItemStack.from(ItemTypes.DIAMOND, 5), 0)))
                .build());

        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 5), entity.getItem(0));

        NbtMap saved = entity.getServerTag();

        PluginChestBlockEntity reloaded = PersistenceFixtures.blockEntity(PLUGIN_CHEST_TYPE, POSITION);
        reloaded.loadAdditionalData(saved);
        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 5), reloaded.getItem(0));
    }
}
