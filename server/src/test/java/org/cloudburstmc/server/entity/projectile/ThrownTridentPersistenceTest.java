package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.ThrownTridentData;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.item.ItemUtils;
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
 * Round-trip tests for the thrown-trident data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class ThrownTridentPersistenceTest {

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
        assertEquals(ThrownTridentData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.THROWN_TRIDENT));

        EntityThrownTrident trident = (EntityThrownTrident) PersistenceFixtures.entity(EntityTypes.THROWN_TRIDENT);
        ItemStack stack = ItemStack.from(ItemTypes.TRIDENT);
        NbtMap input = NbtMap.builder()
                .putCompound("Trident", ItemUtils.serializeItem(stack))
                .putBoolean("DealtDamage", true)
                .putInt("Mystery", 7)
                .build();
        trident.loadAdditionalData(input);

        assertEquals(stack, trident.getTrident());

        NbtMap saved = save(trident);

        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(BASE_KEYS);
        expectedKeys.add("damage");
        expectedKeys.add("inGround");
        expectedKeys.add("PierceLevel");
        expectedKeys.add("pickup");
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
        EntityThrownTrident trident = (EntityThrownTrident) PersistenceFixtures.entity(EntityTypes.THROWN_TRIDENT);

        NbtMap saved = save(trident);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("damage", "inGround", "PierceLevel", "pickup",
                "Trident", "DealtDamage", "identifier"));
        assertEquals(expectedKeys, saved.keySet());

        // Today's save always writes the empty trident, no dealt damage and the base damage.
        assertEquals(ItemUtils.serializeItem(ItemStack.EMPTY), saved.getCompound("Trident"));
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("DealtDamage").getClass()), "DealtDamage must be a byte");
        assertEquals(false, saved.getBoolean("DealtDamage"));
        assertTrue(saved.get("damage") instanceof Float, "damage must be a float");
        assertEquals(8.0f, saved.getFloat("damage"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        EntityThrownTrident trident = (EntityThrownTrident) PersistenceFixtures.entity(EntityTypes.THROWN_TRIDENT);
        ItemStack stack = ItemStack.from(ItemTypes.TRIDENT);
        trident.loadAdditionalData(NbtMap.builder()
                .putCompound("Trident", ItemUtils.serializeItem(stack))
                .putBoolean("DealtDamage", true)
                .build());

        trident.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(stack, trident.getTrident());

        NbtMap saved = save(trident);
        assertEquals(true, saved.getBoolean("DealtDamage"));
    }

    @Test
    void thrownTridentKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityThrownTrident.class, ThrownTridentData.class);
        Set<String> fields = new HashSet<>();
        for (Field field : EntityThrownTrident.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("trident", "dealtDamage")) {
            assertFalse(fields.contains(removed),
                    "EntityThrownTrident keeps " + removed + "; state must live in ThrownTridentData only");
        }
    }
}