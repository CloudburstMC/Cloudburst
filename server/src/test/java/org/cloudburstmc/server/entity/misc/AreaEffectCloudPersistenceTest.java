package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.AreaEffectCloudData;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.potion.PotionEffectDataSerializer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the area-effect-cloud data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types. The duration scale is only
 * written while it differs from one.
 */
class AreaEffectCloudPersistenceTest {

    private static final Set<String> BASE_KEYS = Set.of(
            "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air",
            "OnGround", "Invulnerable", "Scale", "Tags");

    private static final Set<String> CLOUD_KEYS = Set.of(
            "mobEffects", "PotionId", "ParticleColor", "Duration", "DurationOnUse",
            "ReapplicationDelay", "Radius", "RadiusOnUse", "RadiusPerTick", "WaitTime");

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(AreaEffectCloudData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.AREA_EFFECT_CLOUD));

        EntityAreaEffectCloud cloud = (EntityAreaEffectCloud) PersistenceFixtures.entity(EntityTypes.AREA_EFFECT_CLOUD);
        PotionEffect effect = new PotionEffect(EffectTypes.POISON, 200, 1, true, true);
        NbtMap input = NbtMap.builder()
                .putList("mobEffects", NbtType.COMPOUND, List.of(PotionEffectDataSerializer.serialize(effect)))
                .putShort("PotionId", NetworkUtils.potionToNetwork(PotionTypes.POISON))
                .putInt("ParticleColor", 0xff112233)
                .putInt("Duration", 400)
                .putInt("DurationOnUse", 5)
                .putInt("ReapplicationDelay", 30)
                .putFloat("Radius", 4.5f)
                .putFloat("RadiusOnUse", -1.0f)
                .putFloat("RadiusPerTick", -0.01f)
                .putInt("WaitTime", 20)
                .putFloat("PotionDurationScale", 0.5f)
                .putInt("Mystery", 7)
                .build();
        cloud.loadAdditionalData(input);

        assertEquals(PotionTypes.POISON, cloud.getPotionType());
        assertEquals(400, cloud.getDuration());
        assertEquals(4.5f, cloud.getRadius());
        assertEquals(20, cloud.getWaitTime());
        assertEquals(List.of(effect), cloud.getCustomEffects());

        NbtMap saved = save(cloud);

        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(BASE_KEYS);
        expectedKeys.add("identifier");
        assertEquals(expectedKeys, saved.keySet());

        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityAreaEffectCloud cloud = (EntityAreaEffectCloud) PersistenceFixtures.entity(EntityTypes.AREA_EFFECT_CLOUD);

        NbtMap saved = save(cloud);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(CLOUD_KEYS);
        expectedKeys.add("identifier");
        assertEquals(expectedKeys, saved.keySet());

        // Today's save always writes the initialized timers and radius, the water potion,
        // no custom effects and the derived color, and omits the default duration scale.
        assertEquals(List.of(), saved.getList("mobEffects", NbtType.COMPOUND));
        assertEquals(NbtType.COMPOUND, ((NbtList<?>) saved.get("mobEffects")).getType(),
                "empty mobEffects must keep today's COMPOUND element type");
        assertTrue(saved.get("PotionId") instanceof Short, "PotionId must be a short");
        assertEquals(NetworkUtils.potionToNetwork(PotionTypes.WATER), saved.getShort("PotionId"));
        assertTrue(saved.get("ParticleColor") instanceof Integer, "ParticleColor must be an int");
        assertEquals(cloud.getPotionColor(), saved.getInt("ParticleColor"));
        assertEquals(600, saved.getInt("Duration"));
        assertEquals(0, saved.getInt("DurationOnUse"));
        assertEquals(20, saved.getInt("ReapplicationDelay"));
        assertTrue(saved.get("Radius") instanceof Float, "Radius must be a float");
        assertEquals(3.0f, saved.getFloat("Radius"));
        assertEquals(-0.5f, saved.getFloat("RadiusOnUse"));
        assertEquals(-0.005f, saved.getFloat("RadiusPerTick"));
        assertEquals(10, saved.getInt("WaitTime"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        EntityAreaEffectCloud cloud = (EntityAreaEffectCloud) PersistenceFixtures.entity(EntityTypes.AREA_EFFECT_CLOUD);
        cloud.loadAdditionalData(NbtMap.builder()
                .putInt("Duration", 400)
                .putFloat("Radius", 4.5f)
                .putInt("WaitTime", 20)
                .putFloat("PotionDurationScale", 0.5f)
                .build());

        cloud.loadAdditionalData(NbtMap.builder()
                .putShort("PotionId", NetworkUtils.potionToNetwork(PotionTypes.POISON))
                .build());

        NbtMap saved = save(cloud);
        assertEquals(400, saved.getInt("Duration"));
        assertEquals(4.5f, saved.getFloat("Radius"));
        assertEquals(20, saved.getInt("WaitTime"));
        assertEquals(0.5f, saved.getFloat("PotionDurationScale"));
        assertEquals(NetworkUtils.potionToNetwork(PotionTypes.POISON), saved.getShort("PotionId"));
    }

    @Test
    void defaultDurationScaleStaysAbsent() {
        EntityAreaEffectCloud cloud = (EntityAreaEffectCloud) PersistenceFixtures.entity(EntityTypes.AREA_EFFECT_CLOUD);

        NbtMap saved = save(cloud);

        assertFalse(saved.containsKey("PotionDurationScale"), "default scale must not be written");
    }
}
