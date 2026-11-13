package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.vehicle.BoatType;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.CloudBoatData;
import org.cloudburstmc.server.entity.data.CloudChestBoatData;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(CloudBoatData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.BOAT));
        assertEquals(CloudChestBoatData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.CHEST_BOAT));

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

        assertTrue(written.get("WoodType") instanceof Integer, "WoodType must be an int");
        assertEquals(VanillaBoats.definition(BoatType.OAK).variant(), written.getInt("WoodType"));
    }

    @Test
    void woodTypeAlwaysAppliesTodayDefault() {
        EntityBoat boat = boat();
        boat.setBoatType(BoatType.BAMBOO);

        boat.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(BoatType.OAK, boat.getBoatType());
    }

    @Test
    void boatKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityBoat.class, CloudBoatData.class);
        Set<String> fields = new HashSet<>();
        for (Field field : EntityBoat.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("boatType")) {
            assertFalse(fields.contains(removed),
                    "EntityBoat keeps " + removed + "; state must live in CloudBoatData only");
        }
    }
}
