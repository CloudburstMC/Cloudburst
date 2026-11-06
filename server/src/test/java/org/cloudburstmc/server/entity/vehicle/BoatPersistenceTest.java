package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.BoatData;
import org.cloudburstmc.api.entity.data.ChestBoatData;
import org.cloudburstmc.api.entity.vehicle.BoatType;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for the boat branch. The stub fixture cannot build boats
 * ({@code initEntity} needs a live location), so the tests construct them directly
 * without initializing and drive the production load and save paths, asserting the
 * mapped NBT types.
 */
class BoatPersistenceTest {

    private static EntityBoat boat() {
        PersistenceFixtures.ensureInitialized();
        return new EntityBoat(EntityTypes.BOAT,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
    }

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        assertEquals(BoatData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.BOAT));
        assertEquals(ChestBoatData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.CHEST_BOAT));

        EntityBoat boat = boat();
        int variant = VanillaBoats.definition(BoatType.BAMBOO).variant();

        boat.loadAdditionalData(NbtMap.builder()
                .putInt("WoodType", variant)
                .build());

        assertEquals(BoatType.BAMBOO, boat.getBoatType());

        NbtMap written = save(boat);

        assertTrue(written.get("WoodType") instanceof Integer, "WoodType must be an int");
        assertEquals(variant, written.getInt("WoodType"));
    }

    @Test
    void defaultInstanceFillsTodayDefaults() {
        EntityBoat boat = boat();

        NbtMap written = save(boat);

        // Today's save always writes the oak variant for a fresh boat.
        assertTrue(written.get("WoodType") instanceof Integer, "WoodType must be an int");
        assertEquals(VanillaBoats.definition(BoatType.OAK).variant(), written.getInt("WoodType"));
    }

    @Test
    void woodTypeAlwaysAppliesTodayDefault() {
        EntityBoat boat = boat();
        boat.setBoatType(BoatType.BAMBOO);

        // The wood type has no presence bit: it always falls back to oak,
        // exactly as today's getInt-with-default load does.
        boat.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(BoatType.OAK, boat.getBoatType());
    }

    @Test
    void boatKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityBoat.class, BoatData.class);
        Set<String> fields = new HashSet<>();
        for (Field field : EntityBoat.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("boatType")) {
            assertFalse(fields.contains(removed),
                    "EntityBoat keeps " + removed + "; state must live in BoatData only");
        }
    }
}
