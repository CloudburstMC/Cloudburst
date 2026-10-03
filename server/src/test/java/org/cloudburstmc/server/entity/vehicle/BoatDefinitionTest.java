package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.entity.vehicle.BoatType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class BoatDefinitionTest {

    @Test
    void everyHullHasDistinctItemsAndAStableVariant() {
        EnumSet<BoatType> types = EnumSet.noneOf(BoatType.class);
        HashSet<Integer> variants = new HashSet<>();
        HashSet<Object> items = new HashSet<>();

        for (VanillaBoats.Definition definition : VanillaBoats.DEFINITIONS) {
            assertTrue(types.add(definition.type()));
            assertTrue(variants.add(definition.variant()));
            assertTrue(items.add(definition.boatItem()));
            assertTrue(items.add(definition.chestItem()));
            assertSame(definition, VanillaBoats.fromVariant(definition.variant()));
            assertSame(definition, VanillaBoats.definition(definition.type()));
        }

        assertEquals(EnumSet.allOf(BoatType.class), types);
        assertEquals(0, VanillaBoats.definition(BoatType.OAK).variant());
        assertEquals(6, VanillaBoats.definition(BoatType.MANGROVE).variant());
        assertSame(ItemTypes.BAMBOO_RAFT, VanillaBoats.definition(BoatType.BAMBOO).boatItem());
        assertSame(ItemTypes.BAMBOO_CHEST_RAFT, VanillaBoats.definition(BoatType.BAMBOO).chestItem());
        assertThrows(IllegalArgumentException.class, () -> VanillaBoats.fromVariant(-1));
    }

    @Test
    void changingHullUpdatesMetadataAndPickItemTogether() {
        EntityBoat boat = new EntityBoat(EntityTypes.BOAT, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        for (VanillaBoats.Definition definition : VanillaBoats.DEFINITIONS) {
            boat.setBoatType(definition.type());
            assertEquals(definition.type(), boat.getBoatType());
            assertEquals(definition.variant(), boat.getData().require(EntityDataTypes.VARIANT).intValue());
            assertSame(definition.boatItem(), boat.getBoatItem());
        }
        assertThrows(NullPointerException.class, () -> boat.setBoatType(null));
    }

    @Test
    void structuralIntegrityDecreasesAsDamageAccumulates() {
        EntityBoat boat = new EntityBoat(EntityTypes.BOAT, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        assertEquals(40, boat.getData().require(EntityDataTypes.STRUCTURAL_INTEGRITY).intValue());
        assertEquals(0, boat.getDamage());

        boat.setDamage(12);

        assertEquals(28, boat.getData().require(EntityDataTypes.STRUCTURAL_INTEGRITY).intValue());
        assertEquals(12, boat.getDamage());
    }

    @Test
    void raftSeatsTrackTheHullWithoutChangingPassengerCapacity() {
        EntityBoat boat = new EntityBoat(EntityTypes.BOAT, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        Entity passenger = InterfaceProxy.create(Player.class, java.util.Map.of("getBaseOffset", 1.62001f));
        float ordinarySeat = boat.getMountedOffset(passenger).getY();

        assertEquals(0.375f, boat.getBaseOffset());
        assertEquals(1.02001f, ordinarySeat, 0.00001f);
        assertEquals(-0.225f, boat.getPassengerAttachmentPoint(passenger).getY(), 0.00001f);

        boat.setBoatType(BoatType.BAMBOO);

        assertEquals(ordinarySeat + 0.3f, boat.getMountedOffset(passenger).getY(), 0.0001f);
        assertEquals(0.075f, boat.getPassengerAttachmentPoint(passenger).getY(), 0.00001f);
        assertEquals(2, boat.getMaxPassengers());

        EntityChestBoat chestBoat = new EntityChestBoat(EntityTypes.CHEST_BOAT, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        assertEquals(1, chestBoat.getMaxPassengers());
        assertEquals(27, chestBoat.getInventory().size());
    }

    @Test
    void hullOriginAndPassengerPositionUseTheSameCollisionFloor() {
        EntityBoat boat = new HullGeometryBoat(Vector3f.from(4, 63, 8));
        Entity passenger = InterfaceProxy.create(Player.class, java.util.Map.of("getBaseOffset", 1.62001f));

        assertEquals(63, boat.getBoundingBox().getMinY());
        float hullOrigin = boat.getY() + boat.getBaseOffset();
        assertEquals(63.375f, hullOrigin);

        for (BoatType type : BoatType.values()) {
            boat.setBoatType(type);
            float passengerOrigin = boat.getY() + boat.getPassengerAttachmentPoint(passenger).getY() + passenger.getBaseOffset();
            assertEquals(hullOrigin + boat.getMountedOffset(passenger).getY(), passengerOrigin, 0.00001f);
        }
    }

    @Test
    void explicitBoardingIsNotLimitedByAutomaticPickupWidth() {
        EntityBoat boat = new EntityBoat(EntityTypes.BOAT, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        Entity passenger = InterfaceProxy.create(Entity.class, java.util.Map.of("getWidth", 2f));

        assertTrue(boat.canAddPassenger(passenger));
    }

    private static class HullGeometryBoat extends EntityBoat {

        private HullGeometryBoat(Vector3f position) {
            super(EntityTypes.BOAT, Location.from(position, InterfaceProxy.create(Level.class)));
            this.position = position;
            this.recalculateBoundingBox();
        }
    }
}
