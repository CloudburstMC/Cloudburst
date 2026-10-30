package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.CreatureData;
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
 * Round-trip tests for the snow golem data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class SnowGolemPersistenceTest {

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(CreatureData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.SNOW_GOLEM));

        EntitySnowGolem golem = (EntitySnowGolem) PersistenceFixtures.entity(EntityTypes.SNOW_GOLEM);
        golem.loadAdditionalData(NbtMap.builder()
                .putBoolean("Pumpkin", false)
                .putInt("Mystery", 7)
                .build());

        assertTrue(golem.isDerp());

        NbtMap saved = save(golem);

        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("Pumpkin").getClass()), "Pumpkin must be a byte");
        assertEquals(false, saved.getBoolean("Pumpkin"));
        assertEquals(7, saved.getInt("Mystery"));
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntitySnowGolem golem = (EntitySnowGolem) PersistenceFixtures.entity(EntityTypes.SNOW_GOLEM);

        NbtMap saved = save(golem);

        // Today's save always writes Pumpkin true for a fresh golem.
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("Pumpkin").getClass()), "Pumpkin must be a byte");
        assertEquals(true, saved.getBoolean("Pumpkin"));
    }

    @Test
    void absentKeyLeavesStateUnchanged() {
        EntitySnowGolem golem = (EntitySnowGolem) PersistenceFixtures.entity(EntityTypes.SNOW_GOLEM);
        golem.setDerp(true);

        golem.loadAdditionalData(NbtMap.EMPTY);

        assertTrue(golem.isDerp());
    }

    @Test
    void snowGolemKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntitySnowGolem.class, CreatureData.class,
                Map.of(
                        "armor", "an armor view over the live container in data().getArmor()",
                        "offhand", "an offhand view over the live container in data().getOffhand()"));
        Set<String> fields = new HashSet<>();
        for (Field field : EntitySnowGolem.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("pumpkin")) {
            assertFalse(fields.contains(removed),
                    "EntitySnowGolem keeps " + removed + "; state must live in metadata only");
        }
    }
}
