package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.CloudExperienceOrbData;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExperienceOrbPersistenceTest {

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

        EntityExperienceOrb orb = (EntityExperienceOrb) PersistenceFixtures.entity(EntityTypes.XP_ORB);
        NbtMap input = NbtMap.builder()
                .putInt("experience value", 2)
                .putShort("Age", (short) 5)
                .putInt("Count", 3)
                .putShort("PickupDelay", (short) 7)
                .putInt("Mystery", 7)
                .build();
        orb.loadAdditionalData(input);

        assertEquals(2, orb.getExperience());
        assertEquals(3, orb.getCount());

        NbtMap saved = save(orb);

        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(BASE_KEYS);
        expectedKeys.add("identifier");
        assertEquals(expectedKeys, saved.keySet());

        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }
        assertEquals(7, orb.getPickupDelay());
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityExperienceOrb orb = (EntityExperienceOrb) PersistenceFixtures.entity(EntityTypes.XP_ORB);

        NbtMap saved = save(orb);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("experience value", "Age", "Count", "PickupDelay", "identifier"));
        assertEquals(expectedKeys, saved.keySet());

        assertTrue(saved.get("experience value") instanceof Integer, "experience value must be an int");
        assertEquals(1, saved.getInt("experience value"));
        assertTrue(saved.get("Age") instanceof Short, "Age must be a short");
        assertEquals((short) 0, saved.getShort("Age"));
        assertTrue(saved.get("Count") instanceof Integer, "Count must be an int");
        assertEquals(1, saved.getInt("Count"));
        assertTrue(saved.get("PickupDelay") instanceof Short, "PickupDelay must be a short");
        assertEquals((short) 0, saved.getShort("PickupDelay"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        EntityExperienceOrb orb = (EntityExperienceOrb) PersistenceFixtures.entity(EntityTypes.XP_ORB);
        orb.loadAdditionalData(NbtMap.builder()
                .putInt("experience value", 9)
                .putShort("Age", (short) 5)
                .putInt("Count", 4)
                .putShort("PickupDelay", (short) 7)
                .build());

        orb.loadAdditionalData(NbtMap.EMPTY);

        NbtMap saved = save(orb);
        assertEquals(9, saved.getInt("experience value"));
        assertEquals((short) 5, saved.getShort("Age"));
        assertEquals(4, saved.getInt("Count"));
        assertEquals((short) 7, saved.getShort("PickupDelay"));
    }

    @Test
    void experienceOrbKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityExperienceOrb.class, CloudExperienceOrbData.class);
        Set<String> fields = new HashSet<>();
        for (Field field : EntityExperienceOrb.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("count", "pickupDelay")) {
            assertFalse(fields.contains(removed),
                    "EntityExperienceOrb keeps " + removed + "; state must live in CloudExperienceOrbData only");
        }
    }

    @Test
    void experienceValueIsMetadataOnly() {
        assertFalse(hasDtoField(CloudExperienceOrbData.class, "experienceValue"),
                "CloudExperienceOrbData keeps experienceValue; state must live in metadata only");
        assertFalse(hasDtoField(CloudExperienceOrbData.class, "experiencevalue"),
                "CloudExperienceOrbData keeps experienceValue; state must live in metadata only");
    }

    private static boolean hasDtoField(Class<?> type, String name) {
        for (Class<?> current = type; current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void zeroExperienceSurvivesReload() {
        EntityExperienceOrb orb = (EntityExperienceOrb) PersistenceFixtures.entity(EntityTypes.XP_ORB);
        orb.setExperience(0);

        assertEquals(0, orb.getExperience());
        NbtMap saved = save(orb);
        assertEquals(0, saved.getInt("experience value"));

        EntityExperienceOrb restored = (EntityExperienceOrb) PersistenceFixtures.entity(EntityTypes.XP_ORB);
        restored.loadAdditionalData(saved);
        assertEquals(0, restored.getExperience());
    }

    @Test
    void positiveExperienceCanReplaceZero() {
        EntityExperienceOrb orb = (EntityExperienceOrb) PersistenceFixtures.entity(EntityTypes.XP_ORB);
        orb.setExperience(0);
        orb.setExperience(7);

        assertEquals(7, orb.getExperience());
    }

    @Test
    void negativeExperienceIsRejectedWithoutChangingValue() {
        EntityExperienceOrb orb = (EntityExperienceOrb) PersistenceFixtures.entity(EntityTypes.XP_ORB);
        orb.setExperience(7);

        assertThrows(IllegalArgumentException.class, () -> orb.setExperience(-1));
        assertThrows(IllegalArgumentException.class, () -> orb.setExperience(Integer.MIN_VALUE));
        assertEquals(7, orb.getExperience());
    }

    @Test
    void largePickupDelayRemainsNonCollectibleAfterReload() {
        EntityExperienceOrb orb = (EntityExperienceOrb) PersistenceFixtures.entity(EntityTypes.XP_ORB);
        orb.setPickupDelay(40000);

        assertEquals(Short.MAX_VALUE, orb.getPickupDelay());

        NbtMap saved = save(orb);
        assertEquals(Short.MAX_VALUE, saved.get("PickupDelay"));

        EntityExperienceOrb restored = (EntityExperienceOrb) PersistenceFixtures.entity(EntityTypes.XP_ORB);
        restored.loadAdditionalData(saved);
        assertEquals(Short.MAX_VALUE, restored.getPickupDelay());
    }

    @Test
    void consumingOnePickupPreservesTheExperienceValue() {
        EntityExperienceOrb orb = (EntityExperienceOrb) PersistenceFixtures.entity(EntityTypes.XP_ORB);
        orb.setExperience(7);
        orb.setCount(3);

        orb.consumeOne();

        EntityExperienceOrb restored = (EntityExperienceOrb) PersistenceFixtures.entity(EntityTypes.XP_ORB);
        restored.loadAdditionalData(save(orb));
        assertEquals(2, restored.getCount());
        assertEquals(7, restored.getExperience());
    }

    @Test
    void nonPositiveCountIsRejectedWithoutChangingCount() {
        EntityExperienceOrb orb = (EntityExperienceOrb) PersistenceFixtures.entity(EntityTypes.XP_ORB);

        assertThrows(IllegalArgumentException.class, () -> orb.setCount(0));
        assertThrows(IllegalArgumentException.class, () -> orb.setCount(-1));
        assertEquals(1, orb.getCount());
    }
}
