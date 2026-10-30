package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.BoatData;
import org.cloudburstmc.api.entity.data.ChestBoatData;
import org.cloudburstmc.api.entity.vehicle.BoatType;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for the boat branch. The stub fixture cannot build boats
 * ({@code initEntity} needs a live location), so the tests construct them directly
 * without initializing and drive {@code applyData}/{@code fillData}, asserting the
 * mapped NBT types exactly as the production save path would write them.
 */
class BoatPersistenceTest {

    private static EntityBoat boat() {
        PersistenceFixtures.ensureInitialized();
        return new EntityBoat(EntityTypes.BOAT,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
    }

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        assertEquals(BoatData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.BOAT));
        assertEquals(ChestBoatData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.CHEST_BOAT));

        EntityBoat boat = boat();
        int variant = VanillaBoats.definition(BoatType.BAMBOO).variant();

        BoatData loaded = new BoatData();
        loaded.setWoodType(variant);
        boat.applyData(loaded);

        assertEquals(BoatType.BAMBOO, boat.getBoatType());

        BoatData filled = new BoatData();
        boat.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

        assertTrue(written.get("WoodType") instanceof Integer, "WoodType must be an int");
        assertEquals(variant, written.getInt("WoodType"));
    }

    @Test
    void defaultInstanceFillsTodayDefaults() {
        EntityBoat boat = boat();

        BoatData filled = new BoatData();
        boat.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

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
        boat.applyData(new BoatData());

        assertEquals(BoatType.OAK, boat.getBoatType());
    }
}
