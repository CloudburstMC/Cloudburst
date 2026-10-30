package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.HappyGhastData;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the happy ghast data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class HappyGhastPersistenceTest {

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(HappyGhastData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.HAPPY_GHAST));

        EntityHappyGhast ghast = (EntityHappyGhast) PersistenceFixtures.entity(EntityTypes.HAPPY_GHAST);
        ghast.loadAdditionalData(NbtMap.builder()
                .putInt("Age", 6000)
                .putInt("Mystery", 7)
                .build());

        assertEquals(6000, ghast.getAge());

        NbtMap saved = save(ghast);

        assertTrue(saved.get("Age") instanceof Integer, "Age must be an int");
        assertEquals(6000, saved.getInt("Age"));
        assertEquals(7, saved.getInt("Mystery"));
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityHappyGhast ghast = (EntityHappyGhast) PersistenceFixtures.entity(EntityTypes.HAPPY_GHAST);

        NbtMap saved = save(ghast);

        // Today's save always writes Age 0 for a fresh ghast.
        assertTrue(saved.get("Age") instanceof Integer, "Age must be an int");
        assertEquals(0, saved.getInt("Age"));
    }

    @Test
    void absentKeyFallsBackToTodayDefault() {
        EntityHappyGhast ghast = (EntityHappyGhast) PersistenceFixtures.entity(EntityTypes.HAPPY_GHAST);
        ghast.setAge(100);

        // Today's load reads Age with default 0.
        ghast.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(0, ghast.getAge());
    }

    @Test
    void happyGhastKeepsNoDuplicateState() {
        org.cloudburstmc.server.blockentity.DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityHappyGhast.class, HappyGhastData.class,
                java.util.Map.of(
                        "armor", "an armor view over the live container in data().getArmor()",
                        "offhand", "an offhand view over the live container in data().getOffhand()",
                        "age", "the base tick counter, not the persisted Age"));
        java.util.Set<String> fields = new java.util.HashSet<>();
        for (java.lang.reflect.Field field : EntityHappyGhast.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : java.util.Set.of("growingAge")) {
            assertTrue(!fields.contains(removed),
                    "EntityHappyGhast keeps " + removed + "; state must live in HappyGhastData only");
        }
    }
}
