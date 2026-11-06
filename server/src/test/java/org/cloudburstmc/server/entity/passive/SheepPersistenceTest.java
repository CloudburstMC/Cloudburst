package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.CreatureData;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.data.SyncedEntityData;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.COLOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the sheep data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class SheepPersistenceTest {

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(CreatureData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.SHEEP));

        EntitySheep sheep = (EntitySheep) PersistenceFixtures.entity(EntityTypes.SHEEP);
        sheep.loadAdditionalData(NbtMap.builder()
                .putByte("Color", (byte) DyeColor.RED.getWoolData())
                .putBoolean("Sheared", true)
                .putInt("Mystery", 7)
                .build());

        assertEquals(DyeColor.RED, sheep.getColor());
        assertTrue(sheep.isSheared());

        NbtMap saved = save(sheep);

        assertTrue(saved.get("Color") instanceof Byte, "Color must be a byte");
        assertEquals((byte) DyeColor.RED.getWoolData(), saved.getByte("Color"));
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("Sheared").getClass()), "Sheared must be a byte");
        assertTrue(saved.getBoolean("Sheared"));
        assertEquals(7, saved.getInt("Mystery"));
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntitySheep sheep = (EntitySheep) PersistenceFixtures.entity(EntityTypes.SHEEP);

        NbtMap saved = save(sheep);

        // Today's save always writes the (randomized) color byte and Sheared false.
        assertTrue(saved.get("Color") instanceof Byte, "Color must be a byte");
        assertEquals((byte) sheep.getColor().getWoolData(), saved.getByte("Color"));
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("Sheared").getClass()), "Sheared must be a byte");
        assertFalse(saved.getBoolean("Sheared"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        EntitySheep sheep = (EntitySheep) PersistenceFixtures.entity(EntityTypes.SHEEP);
        sheep.setColor(DyeColor.BLUE);
        sheep.setSheared(true);

        sheep.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(DyeColor.BLUE, sheep.getColor());
        assertTrue(sheep.isSheared());
    }

    @Test
    void outOfRangeColorReachesClientUnnormalised() {
        EntitySheep sheep = (EntitySheep) PersistenceFixtures.entity(EntityTypes.SHEEP);
        sheep.loadAdditionalData(NbtMap.builder()
                .putByte("Color", (byte) 20)
                .build());

        // Today's load wrote the raw byte through to the client metadata.
        assertEquals((byte) 20, clientColor(sheep));
        assertEquals(DyeColor.getByWoolData(20), sheep.getColor());

        NbtMap saved = save(sheep);

        // The save still normalises through the wool colour, exactly as today.
        assertTrue(saved.get("Color") instanceof Byte, "Color must be a byte");
        assertEquals((byte) DyeColor.getByWoolData(20).getWoolData(), saved.getByte("Color"));
    }

    private static byte clientColor(EntitySheep sheep) {
        try {
            Field field = CloudEntity.class.getDeclaredField("data");
            field.setAccessible(true);
            return ((SyncedEntityData) field.get(sheep)).require(COLOR);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void sheepKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntitySheep.class, CreatureData.class,
                Map.of(
                        "armor", "an armor view over the live container in data().getArmor()",
                        "offhand", "an offhand view over the live container in data().getOffhand()"));
        Set<String> fields = new HashSet<>();
        for (Field field : EntitySheep.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("color", "sheared")) {
            assertFalse(fields.contains(removed),
                    "EntitySheep keeps " + removed + "; state must live in metadata only");
        }
    }
}
