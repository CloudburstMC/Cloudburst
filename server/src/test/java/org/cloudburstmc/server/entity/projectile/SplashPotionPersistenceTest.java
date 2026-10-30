package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.SplashPotionData;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.network.NetworkUtils;
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
 * Round-trip tests for the splash-potion data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types. The lingering potion shares
 * this path without its own keys.
 */
class SplashPotionPersistenceTest {

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
        assertEquals(SplashPotionData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.SPLASH_POTION));
        assertEquals(SplashPotionData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.LINGERING_POTION));

        EntitySplashPotion potion = (EntitySplashPotion) PersistenceFixtures.entity(EntityTypes.SPLASH_POTION);
        PotionType type = PotionTypes.STRONG_POISON;
        NbtMap input = NbtMap.builder()
                .putShort("PotionId", NetworkUtils.potionToNetwork(type))
                .putInt("Mystery", 7)
                .build();
        potion.loadAdditionalData(input);

        assertEquals(type, potion.getPotionType());

        NbtMap saved = save(potion);

        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(BASE_KEYS);
        expectedKeys.add("damage");
        expectedKeys.add("identifier");
        assertEquals(expectedKeys, saved.keySet());

        assertTrue(saved.get("PotionId") instanceof Short, "PotionId must be a short");
        assertEquals(NetworkUtils.potionToNetwork(type), saved.getShort("PotionId"));
        assertEquals(7, saved.getInt("Mystery"));
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntitySplashPotion potion = (EntitySplashPotion) PersistenceFixtures.entity(EntityTypes.SPLASH_POTION);

        NbtMap saved = save(potion);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("PotionId", "damage", "identifier"));
        assertEquals(expectedKeys, saved.keySet());

        // Today's save always writes the water potion.
        assertTrue(saved.get("PotionId") instanceof Short, "PotionId must be a short");
        assertEquals(NetworkUtils.potionToNetwork(PotionTypes.WATER), saved.getShort("PotionId"));
    }

    @Test
    void absentKeyLeavesStateUnchanged() {
        EntitySplashPotion potion = (EntitySplashPotion) PersistenceFixtures.entity(EntityTypes.SPLASH_POTION);
        potion.setPotionType(PotionTypes.STRONG_POISON);

        potion.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(PotionTypes.STRONG_POISON, potion.getPotionType());
    }

    @Test
    void splashPotionKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntitySplashPotion.class, SplashPotionData.class);
        Set<String> fields = new HashSet<>();
        for (Field field : EntitySplashPotion.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("type")) {
            assertFalse(fields.contains(removed),
                    "EntitySplashPotion keeps " + removed + "; state must live in SplashPotionData only");
        }
    }
}