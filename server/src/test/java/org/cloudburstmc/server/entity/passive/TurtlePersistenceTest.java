package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.TurtleData;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the turtle data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class TurtlePersistenceTest {

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(TurtleData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.TURTLE));

        EntityTurtle turtle = (EntityTurtle) PersistenceFixtures.entity(EntityTypes.TURTLE);
        turtle.loadAdditionalData(NbtMap.builder()
                .putInt("Age", 100)
                .putInt("HomePosX", 5)
                .putInt("HomePosY", 6)
                .putInt("HomePosZ", 7)
                .putInt("Mystery", 7)
                .build());

        assertEquals(100, turtle.getAge());

        NbtMap saved = save(turtle);

        assertTrue(saved.get("Age") instanceof Integer, "Age must be an int");
        assertEquals(100, saved.getInt("Age"));
        assertTrue(saved.get("HomePosX") instanceof Integer, "HomePosX must be an int");
        assertEquals(5, saved.getInt("HomePosX"));
        assertTrue(saved.get("HomePosY") instanceof Integer, "HomePosY must be an int");
        assertEquals(6, saved.getInt("HomePosY"));
        assertTrue(saved.get("HomePosZ") instanceof Integer, "HomePosZ must be an int");
        assertEquals(7, saved.getInt("HomePosZ"));
        assertEquals(7, saved.getInt("Mystery"));
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityTurtle turtle = (EntityTurtle) PersistenceFixtures.entity(EntityTypes.TURTLE);

        NbtMap saved = save(turtle);

        // Today's save always writes Age 0 and the spawn position as home.
        assertTrue(saved.get("Age") instanceof Integer, "Age must be an int");
        assertEquals(0, saved.getInt("Age"));
        assertTrue(saved.get("HomePosX") instanceof Integer, "HomePosX must be an int");
        assertEquals(0, saved.getInt("HomePosX"));
        assertTrue(saved.get("HomePosY") instanceof Integer, "HomePosY must be an int");
        assertEquals(0, saved.getInt("HomePosY"));
        assertTrue(saved.get("HomePosZ") instanceof Integer, "HomePosZ must be an int");
        assertEquals(0, saved.getInt("HomePosZ"));
    }

    @Test
    void absentHomeFallsBackToLoadedPosition() {
        EntityTurtle turtle = (EntityTurtle) PersistenceFixtures.entity(EntityTypes.TURTLE);
        turtle.setAge(100);

        // Today's load reads Age with default 0; home falls back to the loaded position.
        turtle.loadAdditionalData(NbtMap.builder()
                .putList("Pos", NbtType.FLOAT, List.of(5f, 6f, 7f))
                .build());

        assertEquals(0, turtle.getAge());

        NbtMap saved = save(turtle);
        assertEquals(5, saved.getInt("HomePosX"));
        assertEquals(6, saved.getInt("HomePosY"));
        assertEquals(7, saved.getInt("HomePosZ"));
    }

    @Test
    void absentAgeResetsToTodayDefault() {
        EntityTurtle turtle = (EntityTurtle) PersistenceFixtures.entity(EntityTypes.TURTLE);
        turtle.setAge(100);

        turtle.loadAdditionalData(NbtMap.builder()
                .putInt("HomePosX", 1)
                .putInt("HomePosY", 2)
                .putInt("HomePosZ", 3)
                .build());

        assertEquals(0, turtle.getAge());
    }

    @Test
    void turtleKeepsNoDuplicateState() {
        org.cloudburstmc.server.blockentity.DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityTurtle.class, TurtleData.class,
                java.util.Map.of(
                        "armor", "an armor view over the live container in data().getArmor()",
                        "offhand", "an offhand view over the live container in data().getOffhand()",
                        "age", "the base tick counter, not the persisted Age"));
        java.util.Set<String> fields = new java.util.HashSet<>();
        for (java.lang.reflect.Field field : EntityTurtle.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : java.util.Set.of("growingAge", "homePosition")) {
            assertTrue(!fields.contains(removed),
                    "EntityTurtle keeps " + removed + "; state must live in TurtleData only");
        }
    }
}
