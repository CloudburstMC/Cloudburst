package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.api.util.data.FireworkData;
import org.cloudburstmc.api.util.data.FireworkData.FireworkExplosion;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.CloudFireworkRocketData;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FireworkRocketPersistenceTest {

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

        EntityFireworkRocket rocket = (EntityFireworkRocket) PersistenceFixtures.entity(EntityTypes.FIREWORK_ROCKET);
        NbtMap input = NbtMap.builder()
                .putInt("Life", 10)
                .putInt("LifeTime", 100)
                .putBoolean("ShotAtAngle", true)
                .putInt("Mystery", 7)
                .build();
        rocket.loadAdditionalData(input);

        assertEquals(10, rocket.getTicksFlown());
        assertEquals(100, rocket.getTicksToDetonate());

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
        EntityFireworkRocket rocket = (EntityFireworkRocket) PersistenceFixtures.entity(EntityTypes.FIREWORK_ROCKET);

        NbtMap saved = save(rocket);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("Life", "LifeTime", "ShotAtAngle", "damage", "identifier"));
        assertEquals(expectedKeys, saved.keySet());

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
        EntityFireworkRocket rocket = (EntityFireworkRocket) PersistenceFixtures.entity(EntityTypes.FIREWORK_ROCKET);
        rocket.setTicksFlown(12);
        rocket.setTicksToDetonate(90);
        rocket.setShotAtAngle(true);

        rocket.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(12, rocket.getTicksFlown());
        assertEquals(90, rocket.getTicksToDetonate());
        assertTrue(save(rocket).getBoolean("ShotAtAngle"));
    }

    @Test
    void configuredPayloadAndLifetimeSurviveReload() {
        EntityFireworkRocket rocket = (EntityFireworkRocket) PersistenceFixtures.entity(EntityTypes.FIREWORK_ROCKET);
        FireworkExplosion explosion = FireworkExplosion.of(List.of(DyeColor.RED), List.of(),
                false, true, FireworkExplosion.ExplosionType.STAR_SHAPED, 0);
        rocket.setFireworkData(FireworkData.of(List.of(explosion), (byte) 2));
        rocket.setTicksFlown(12);
        rocket.setTicksToDetonate(90);

        EntityFireworkRocket restored = (EntityFireworkRocket) PersistenceFixtures.entity(EntityTypes.FIREWORK_ROCKET);
        restored.loadAdditionalData(save(rocket));

        assertEquals(12, restored.getTicksFlown());
        assertEquals(90, restored.getTicksToDetonate());
        FireworkData payload = restored.getFireworkData();
        assertEquals(2, payload.getFlightLevel());
        FireworkExplosion restoredExplosion = assertDoesNotThrow(() -> payload.getExplosions().getFirst());
        assertEquals(List.of(DyeColor.RED), restoredExplosion.getColors());
        assertEquals(FireworkExplosion.ExplosionType.STAR_SHAPED, restoredExplosion.getType());
        assertTrue(restoredExplosion.isTrail());
    }

    @Test
    void fireworksRocketKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityFireworkRocket.class, CloudFireworkRocketData.class);
        Set<String> fields = new HashSet<>();
        for (Field field : EntityFireworkRocket.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("ticksFlown", "ticksToDetonate", "shotAtAngle")) {
            assertFalse(fields.contains(removed),
                    "EntityFireworkRocket keeps " + removed + "; state must live in CloudFireworkRocketData only");
        }
    }
}
