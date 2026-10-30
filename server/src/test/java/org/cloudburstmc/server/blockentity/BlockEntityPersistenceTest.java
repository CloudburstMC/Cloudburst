package org.cloudburstmc.server.blockentity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.nbt.NbtClient;
import org.cloudburstmc.api.nbt.NbtMapper;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
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

    private static final BlockEntityType<MigratedBlockEntity> MIGRATED_TYPE =
            BlockEntityType.from("test:migrated", MigratedBlockEntity.class);
    private static final BlockEntityType<LegacyBlockEntity> LEGACY_TYPE =
            BlockEntityType.from("test:legacy", LegacyBlockEntity.class);

    @BeforeAll
    static void registerTestTypes() {
        CloudBlockEntityRegistry registry = CloudBlockEntityRegistry.get();
        registry.register(MIGRATED_TYPE, MigratedBlockEntity::new, "TestMigrated",
                Set.of(BlockTypes.CHEST), MigratedData.class);
        registry.register(LEGACY_TYPE, LegacyBlockEntity::new, "TestLegacy",
                Set.of(BlockTypes.CHEST));
        registry.close();
    }

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

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    public static class MigratedData extends BlockEntityData {

        @NbtClient
        @JsonProperty("Count")
        private int count;

        public MigratedData() {
        }
    }

    public static class MigratedBlockEntity extends BaseBlockEntity {

        private int count;
        private boolean flag;

        public MigratedBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
            super(type, chunk, position);
        }

        @Override
        protected void applyData(BlockEntityData data) {
            super.applyData(data);
            this.count = ((MigratedData) data).getCount();
        }

        @Override
        protected void fillData(BlockEntityData data) {
            super.fillData(data);
            ((MigratedData) data).setCount(this.count);
        }

        @Override
        protected void loadCustom(NbtMap tag) {
            tag.listenForBoolean("Flag", value -> this.flag = value);
        }

        @Override
        protected void saveCustom(NbtMapBuilder tag) {
            if (this.flag) {
                tag.putBoolean("Flag", true);
            }
        }

        @Override
        protected Set<String> customKeys() {
            return Set.of("Flag");
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }
    }

    public static class LegacyBlockEntity extends BaseBlockEntity {

        public LegacyBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
            super(type, chunk, position);
        }
    }
}
