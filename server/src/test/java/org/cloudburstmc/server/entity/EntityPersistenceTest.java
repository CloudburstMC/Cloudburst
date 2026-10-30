package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.nbt.NbtMapper;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.cloudburstmc.server.testutil.TestEntities;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the entity data path: unknown keys survive in the
 * raw tag, DTO properties write exactly, conditional keys clear cleanly,
 * plugin data stays server-side, and every registration round-trips.
 */
class EntityPersistenceTest {

    @Test
    void entityUnknownKeysSurvive() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder()
                .putList("Pos", NbtType.FLOAT, List.of(1f, 2f, 3f))
                .putList("Motion", NbtType.FLOAT, List.of(0f, 0f, 0f))
                .putList("Rotation", NbtType.FLOAT, List.of(0f, 0f))
                .putInt("Mystery", 7)
                .build());

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        assertEquals(7, saved.getInt("Mystery"));
    }

    @Test
    void clearedNameTagStaysCleared() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("CustomName", "A")
                .putBoolean("CustomNameVisible", true)
                .putBoolean("CustomNameAlwaysVisible", true)
                .build());

        assertEquals("A", entity.getNameTag());

        entity.setNameTag("");

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        assertFalse(saved.containsKey("CustomName"));
        assertFalse(saved.containsKey("CustomNameVisible"));
        assertFalse(saved.containsKey("CustomNameAlwaysVisible"));
    }

    @Test
    void absentCustomNameKeepsExistingNameTag() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.setNameTag("Steve");
        entity.setNameTagVisible(true);
        entity.setNameTagAlwaysVisible(true);

        entity.loadAdditionalData(NbtMap.EMPTY);

        assertEquals("Steve", entity.getNameTag());
        assertTrue(entity.isNameTagVisible());
        assertTrue(entity.isNameTagAlwaysVisible());
    }

    @Test
    void entityPluginDataSurvives() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.EMPTY);

        entity.setPluginData("myplugin", NbtMap.builder().putInt("a", 1).build());

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        assertEquals(1, saved.getCompound(NbtMapper.PLUGIN_DATA_KEY).getCompound("myplugin").getInt("a"));

        CloudEntity reloaded = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        reloaded.loadAdditionalData(saved);

        assertEquals(1, reloaded.getPluginData("myplugin").orElseThrow().getInt("a"));

        reloaded.removePluginData("myplugin");

        assertTrue(reloaded.getPluginData("myplugin").isEmpty());

        NbtMapBuilder afterRemove = NbtMap.builder();
        reloaded.saveAdditionalData(afterRemove);
        assertFalse(afterRemove.build().containsKey(NbtMapper.PLUGIN_DATA_KEY));
    }

    @Test
    void pluginDataMarksChunkDirty() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.EMPTY);

        // Fixture entities never run initialize, so they have no chunk; attach a
        // stub-backed chunk the way the block-entity fixture builds one.
        entity.chunk = new CloudChunk(0, 0, InterfaceProxy.create(Level.class));

        long revision = entity.chunk.captureSaveRevision();
        entity.setPluginData("myplugin", NbtMap.builder().putInt("a", 1).build());
        assertNotEquals(revision, entity.chunk.captureSaveRevision());

        revision = entity.chunk.captureSaveRevision();
        entity.removePluginData("myplugin");
        assertNotEquals(revision, entity.chunk.captureSaveRevision());
    }

    @Test
    void pluginDataWithoutChunkDoesNotThrow() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.EMPTY);

        entity.setPluginData("myplugin", NbtMap.builder().putInt("a", 1).build());
        entity.removePluginData("myplugin");
    }

    @Test
    void posIsWrittenAsFloatList() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.EMPTY);
        entity.setPosition(Vector3f.from(1f, 2f, 3f));

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        Object raw = saved.get("Pos");
        assertTrue(raw instanceof NbtList<?>, "Pos must be a list");
        assertEquals(NbtType.FLOAT, ((NbtList<?>) raw).getType());
        assertEquals(List.of(1f, 2f, 3f), saved.getList("Pos", NbtType.FLOAT));
    }

    @Test
    void livingHealthRoundTripsAsFloat() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        entity.loadAdditionalData(NbtMap.EMPTY);
        ((EntityLiving) entity).setHealth(5f);

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        Object raw = saved.get("Health");
        assertTrue(raw instanceof Float, "Health must be a float, was: " + raw);
        assertEquals(5f, saved.getFloat("Health"));

        CloudEntity reloaded = (CloudEntity) PersistenceFixtures.entity(EntityTypes.PIG);
        reloaded.loadAdditionalData(saved);
        assertEquals(5f, ((EntityLiving) reloaded).getHealth());
    }

    @Test
    void legacyCreatureRegistrationRoundTripsWithCreatureData() {
        CloudEntity entity = (CloudEntity) PersistenceFixtures.entity(TestEntities.LEGACY_CREATURE_TYPE);

        // The old overload registered the base data class; the runtime class's
        // minimum resolves the effective class so the creature cast cannot fail.
        assertEquals(EntityData.class,
                CloudEntityRegistry.get().getDataClass(TestEntities.LEGACY_CREATURE_TYPE));
        assertEquals(org.cloudburstmc.api.entity.data.CreatureData.class, entity.dataClass());

        entity.loadAdditionalData(NbtMap.builder()
                .putList("Pos", NbtType.FLOAT, List.of(1f, 2f, 3f))
                .putList("Motion", NbtType.FLOAT, List.of(0f, 0f, 0f))
                .putList("Rotation", NbtType.FLOAT, List.of(0f, 0f))
                .build());

        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        NbtMap saved = builder.build();

        CloudEntity second = (CloudEntity) PersistenceFixtures.entity(TestEntities.LEGACY_CREATURE_TYPE);
        second.loadAdditionalData(saved);

        NbtMapBuilder again = NbtMap.builder();
        second.saveAdditionalData(again);
        assertEquals(saved, again.build());
    }

    @Test
    void allRegisteredEntityTypesBuildAndRoundTrip() {
        List<String> failures = new ArrayList<>();
        for (EntityType<?> type : CloudEntityRegistry.get().values()) {
            if (type == EntityTypes.PLAYER || Player.class.isAssignableFrom(type.getEntityClass())) {
                continue;
            }
            if (KNOWN_UNSTABLE_TYPES.contains(type.getId().toString())) {
                continue;
            }
            CloudEntity entity;
            try {
                entity = (CloudEntity) PersistenceFixtures.entity(type);
            } catch (Exception e) {
                failures.add(type.getId() + ": construction failed (" + e + ")");
                continue;
            }
            if ("minecraft".equals(type.getId().getNamespace())
                    && !entity.minimumDataClass().isAssignableFrom(CloudEntityRegistry.get().getDataClass(type))) {
                failures.add(type.getId() + ": registered " + CloudEntityRegistry.get().getDataClass(type).getName()
                        + " is not usable by runtime minimum " + entity.minimumDataClass().getName());
                continue;
            }
            NbtMap saved;
            try {
                NbtMapBuilder builder = NbtMap.builder();
                entity.saveAdditionalData(builder);
                saved = builder.build();
            } catch (Exception e) {
                failures.add(type.getId() + ": save failed (" + e + ")");
                continue;
            }

            CloudEntity reloaded;
            try {
                reloaded = (CloudEntity) PersistenceFixtures.entity(type);
            } catch (Exception e) {
                failures.add(type.getId() + ": reload construction failed (" + e + ")");
                continue;
            }
            try {
                reloaded.loadAdditionalData(saved);
            } catch (Exception e) {
                failures.add(type.getId() + ": load failed (" + e + ")");
                continue;
            }
            try {
                NbtMapBuilder second = NbtMap.builder();
                reloaded.saveAdditionalData(second);
                // Every type is migrated now, so a reload must save identically.
                assertEquals(saved, second.build(), type.getId() + " must save identically after a reload");
            } catch (AssertionError e) {
                failures.add(type.getId() + ": resave differs (" + e.getMessage() + ")");
            } catch (Exception e) {
                failures.add(type.getId() + ": resave failed (" + e + ")");
            }
        }
        if (!failures.isEmpty()) {
            System.out.println("Unstable entity types (" + failures.size() + "):");
            failures.forEach(System.out::println);
        }
        assertTrue(failures.isEmpty(), "Entity round-trip failures: " + failures);
        assertEquals(org.cloudburstmc.api.entity.data.CreatureData.class,
                CloudEntityRegistry.get().getDataClass(EntityTypes.PIG));
        assertEquals(org.cloudburstmc.api.entity.data.PaintingData.class,
                CloudEntityRegistry.get().getDataClass(EntityTypes.PAINTING));
    }

    /**
     * Types whose load or init paths need a live world (server, level or game rules)
     * and cannot run in the stub fixture. Loading a creeper fires its power event
     * through the server; the others touch the level during initialization.
     */
    private static final java.util.Set<String> KNOWN_UNSTABLE_TYPES = java.util.Set.of(
            // load fires CreeperPowerEvent via server
            "minecraft:creeper",
            // init reads level dimension/end fight
            "minecraft:ender_dragon",
            // init captures location/last event state needing a level
            "minecraft:boat",
            "minecraft:chest_boat",
            // init reads level game rules and server difficulty
            "minecraft:lightning_bolt");
}
