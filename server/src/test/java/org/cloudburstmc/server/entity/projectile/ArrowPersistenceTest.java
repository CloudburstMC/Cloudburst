package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.ArrowData;
import org.cloudburstmc.api.entity.projectile.ArrowPickupStatus;
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
 * Round-trip tests for the arrow data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class ArrowPersistenceTest {

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
        assertEquals(ArrowData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.ARROW));

        EntityArrow arrow = (EntityArrow) PersistenceFixtures.entity(EntityTypes.ARROW);
        ItemStack stack = ItemStack.from(ItemTypes.ARROW);
        NbtMap input = NbtMap.builder()
                .putFloat("damage", 2.0f)
                .putBoolean("inGround", true)
                .putByte("PierceLevel", (byte) 5)
                .putInt("inBlockX", 1)
                .putInt("inBlockY", 2)
                .putInt("inBlockZ", 3)
                .putByte("pickup", (byte) 0)
                .putCompound("Item", ItemUtils.serializeItem(stack))
                .putInt("PotionGroundTicks", 5)
                .putInt("Mystery", 7)
                .build();
        arrow.loadAdditionalData(input);

        assertEquals(ArrowPickupStatus.DISALLOWED, arrow.getPickupStatus());
        assertEquals(stack, arrow.getItemStack());

        NbtMap saved = save(arrow);

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
        EntityArrow arrow = (EntityArrow) PersistenceFixtures.entity(EntityTypes.ARROW);

        NbtMap saved = save(arrow);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("damage", "inGround", "PierceLevel", "pickup",
                "Item", "PotionGroundTicks", "identifier"));
        assertEquals(expectedKeys, saved.keySet());

        // Today's save always writes the base damage, the flying state, open pickup,
        // a plain arrow and fresh ground ticks.
        assertTrue(saved.get("damage") instanceof Float, "damage must be a float");
        assertEquals(2.0f, saved.getFloat("damage"));
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("inGround").getClass()), "inGround must be a byte");
        assertFalse(saved.getBoolean("inGround"));
        assertEquals((byte) 0, saved.getByte("PierceLevel"));
        assertEquals((byte) 1, saved.getByte("pickup"));
        assertEquals(ItemUtils.serializeItem(ItemStack.from(ItemTypes.ARROW)), saved.getCompound("Item"));
        assertTrue(saved.get("PotionGroundTicks") instanceof Integer, "PotionGroundTicks must be an int");
        assertEquals(0, saved.getInt("PotionGroundTicks"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        EntityArrow arrow = (EntityArrow) PersistenceFixtures.entity(EntityTypes.ARROW);
        arrow.setPierceLevel(3);
        arrow.setPickupStatus(ArrowPickupStatus.CREATIVE_ONLY);
        arrow.loadAdditionalData(NbtMap.builder()
                .putBoolean("inGround", true)
                .putCompound("Item", ItemUtils.serializeItem(ItemStack.from(ItemTypes.ARROW)))
                .build());

        arrow.loadAdditionalData(NbtMap.EMPTY);

        NbtMap saved = save(arrow);
        // Presence-sensitive keys survive; the pickup rule always falls back to open pickup.
        assertEquals(3, arrow.getPierceLevel());
        assertEquals(ArrowPickupStatus.ALLOWED, arrow.getPickupStatus());
        assertEquals((byte) 1, saved.getByte("pickup"));
        assertTrue(saved.getBoolean("inGround"));
    }

    @Test
    void absentPotionGroundTicksResetToZero() {
        EntityArrow arrow = (EntityArrow) PersistenceFixtures.entity(EntityTypes.ARROW);
        arrow.loadAdditionalData(NbtMap.builder()
                .putCompound("Item", ItemUtils.serializeItem(ItemStack.from(ItemTypes.ARROW)))
                .putInt("PotionGroundTicks", 500)
                .build());

        // The ground ticks have a load default of zero, so they reset when absent.
        arrow.loadAdditionalData(NbtMap.builder()
                .putCompound("Item", ItemUtils.serializeItem(ItemStack.from(ItemTypes.ARROW)))
                .build());

        NbtMap saved = save(arrow);
        assertEquals(0, saved.getInt("PotionGroundTicks"));
    }

    @Test
    void arrowKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityArrow.class, ArrowData.class);
        Set<String> fields = new HashSet<>();
        for (Field field : EntityArrow.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("itemStack", "potionGroundTicks")) {
            assertFalse(fields.contains(removed),
                    "EntityArrow keeps " + removed + "; state must live in ArrowData only");
        }
    }
}
