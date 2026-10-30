package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.PrimedTntData;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the primed-TNT data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class PrimedTntPersistenceTest {

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
        assertEquals(PrimedTntData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.TNT));

        EntityPrimedTnt tnt = (EntityPrimedTnt) PersistenceFixtures.entity(EntityTypes.TNT);
        NbtMap input = NbtMap.builder()
                .putInt("Fuse", 40)
                .putInt("Mystery", 7)
                .build();
        tnt.loadAdditionalData(input);

        assertEquals(40, tnt.getFuse());

        NbtMap saved = save(tnt);

        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(BASE_KEYS);
        expectedKeys.add("identifier");
        assertEquals(expectedKeys, saved.keySet());

        assertTrue(saved.get("Fuse") instanceof Integer, "Fuse must be an int");
        assertEquals(40, saved.getInt("Fuse"));
        assertEquals(7, saved.getInt("Mystery"));
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityPrimedTnt tnt = (EntityPrimedTnt) PersistenceFixtures.entity(EntityTypes.TNT);

        NbtMap saved = save(tnt);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("Fuse", "identifier"));
        assertEquals(expectedKeys, saved.keySet());

        // Today's save always writes the 80-tick fuse of a fresh block.
        assertTrue(saved.get("Fuse") instanceof Integer, "Fuse must be an int");
        assertEquals(80, saved.getInt("Fuse"));
    }

    @Test
    void absentKeyLeavesStateUnchanged() {
        EntityPrimedTnt tnt = (EntityPrimedTnt) PersistenceFixtures.entity(EntityTypes.TNT);
        tnt.setFuse(10);

        tnt.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(10, tnt.getFuse());
    }

    @Test
    void primedTntKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityPrimedTnt.class, PrimedTntData.class);
        Set<String> fields = new HashSet<>();
        for (Field field : EntityPrimedTnt.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("fuse")) {
            assertFalse(fields.contains(removed),
                    "EntityPrimedTnt keeps " + removed + "; state must live in PrimedTntData only");
        }
    }
}