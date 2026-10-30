package org.cloudburstmc.server.testutil;

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
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.blockentity.ChestBlockEntity;

import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * Test-only block-entity types used by persistence round-trip tests. The ids are
 * intentionally in the {@code test} namespace; they stay registered in the shared
 * singleton for the JVM's lifetime, so all registration happens in exactly one place
 * ({@link PersistenceFixtures}).
 */
public final class TestBlockEntities {

    public static final BlockEntityType<MigratedBlockEntity> MIGRATED_TYPE =
            BlockEntityType.from("test:migrated", MigratedBlockEntity.class);
    public static final BlockEntityType<LegacyBlockEntity> LEGACY_TYPE =
            BlockEntityType.from("test:legacy", LegacyBlockEntity.class);
    public static final BlockEntityType<PluginChestBlockEntity> PLUGIN_CHEST_TYPE =
            BlockEntityType.from("test:plugin_chest", PluginChestBlockEntity.class);

    private TestBlockEntities() {
    }

    public static void registerAll(org.cloudburstmc.server.registry.CloudBlockEntityRegistry registry) {
        registry.register(MIGRATED_TYPE, MigratedBlockEntity::new, "TestMigrated",
                Set.of(BlockTypes.CHEST), MigratedData.class);
        registry.register(LEGACY_TYPE, LegacyBlockEntity::new, "TestLegacy",
                Set.of(BlockTypes.CHEST));
        // The old four-argument overload defaults to the base data class; the
        // runtime class's minimum resolves the effective class to ChestData.
        registry.register(PLUGIN_CHEST_TYPE, PluginChestBlockEntity::new, "TestPluginChest",
                Set.of(BlockTypes.CHEST));
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

    public static class PluginChestBlockEntity extends ChestBlockEntity {

        public PluginChestBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
            super(type, chunk, position);
        }
    }
}
