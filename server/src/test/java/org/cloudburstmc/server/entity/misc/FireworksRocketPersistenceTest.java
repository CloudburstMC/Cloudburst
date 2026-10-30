package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.FireworksRocketData;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the fireworks-rocket data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class FireworksRocketPersistenceTest {

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
        assertEquals(FireworksRocketData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.FIREWORKS_ROCKET));

        EntityFireworksRocket rocket = (EntityFireworksRocket) PersistenceFixtures.entity(EntityTypes.FIREWORKS_ROCKET);
        NbtMap input = NbtMap.builder()
                .putInt("Life", 10)
                .putInt("LifeTime", 100)
                .putBoolean("ShotAtAngle", true)
                .putInt("Mystery", 7)
                .build();
        rocket.loadAdditionalData(input);

        assertEquals(10, rocket.getLife());
        assertEquals(100, rocket.getLifetime());

        NbtMap saved = save(rocket);

        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(BASE_KEYS);
        expectedKeys.add("damage");
        expectedKeys.add("identifier");
        assertEquals(expectedKeys, saved.keySet());

        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("ShotAtAngle").getClass()), "ShotAtAngle must be a byte");
        assertTrue(saved.getBoolean("ShotAtAngle"));
        assertEquals(10, saved.getInt("Life"));
        assertEquals(100, saved.getInt("LifeTime"));
        assertEquals(7, saved.getInt("Mystery"));
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityFireworksRocket rocket = (EntityFireworksRocket) PersistenceFixtures.entity(EntityTypes.FIREWORKS_ROCKET);

        NbtMap saved = save(rocket);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("Life", "LifeTime", "ShotAtAngle", "damage", "identifier"));
        assertEquals(expectedKeys, saved.keySet());

        // Today's save always writes a fresh life, a randomized lifetime and a level flight.
        assertTrue(saved.get("Life") instanceof Integer, "Life must be an int");
        assertEquals(0, saved.getInt("Life"));
        assertTrue(saved.get("LifeTime") instanceof Integer, "LifeTime must be an int");
        assertTrue(saved.getInt("LifeTime") >= 20 && saved.getInt("LifeTime") <= 31,
                "LifeTime must be today's randomized range, was " + saved.getInt("LifeTime"));
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("ShotAtAngle").getClass()), "ShotAtAngle must be a byte");
        assertFalse(saved.getBoolean("ShotAtAngle"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        EntityFireworksRocket rocket = (EntityFireworksRocket) PersistenceFixtures.entity(EntityTypes.FIREWORKS_ROCKET);
        rocket.setLife(12);
        rocket.setLifetime(90);
        rocket.setShotAtAngle(true);

        rocket.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(12, rocket.getLife());
        assertEquals(90, rocket.getLifetime());
        assertTrue(save(rocket).getBoolean("ShotAtAngle"));
    }

    @Test
    void fireworksRocketKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityFireworksRocket.class, FireworksRocketData.class,
                Map.of("firework", "the live firework item, never persisted; rebuilt through setFireworkData"));
        Set<String> fields = new HashSet<>();
        for (Field field : EntityFireworksRocket.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("life", "lifetime", "shotAtAngle")) {
            assertFalse(fields.contains(removed),
                    "EntityFireworksRocket keeps " + removed + "; state must live in FireworksRocketData only");
        }
    }
}