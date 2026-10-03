package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntityVehicleRelationshipTest {

    @Test
    void comparesTheEntireVehicleChain() {
        CloudEntity vehicle = entity();
        CloudEntity passenger = entity();
        CloudEntity nestedPassenger = entity();
        CloudEntity otherPassenger = entity();
        passenger.vehicle = vehicle;
        nestedPassenger.vehicle = passenger;
        otherPassenger.vehicle = vehicle;

        assertTrue(vehicle.sharesRootVehicle(passenger));
        assertTrue(passenger.sharesRootVehicle(vehicle));
        assertTrue(nestedPassenger.sharesRootVehicle(otherPassenger));
        assertFalse(vehicle.sharesRootVehicle(entity()));
        assertTrue(vehicle.sharesRootVehicle(vehicle));
    }

    @Test
    void rejectsSelfMountingAndMountingADescendant() {
        CloudEntity vehicle = entity();
        CloudEntity passenger = entity();
        passenger.vehicle = vehicle;

        assertFalse(vehicle.mount(vehicle));
        assertFalse(vehicle.mount(passenger));
        assertNull(vehicle.getVehicle());
        assertSame(vehicle, passenger.getVehicle());
    }

    @Test
    void passengerSnapshotsCannotChangeVehicleOwnership() {
        CloudEntity vehicle = entity();
        CloudEntity passenger = entity();
        vehicle.passengers.add(passenger);
        List<Entity> snapshot = vehicle.getPassengers();

        assertSame(passenger, vehicle.getControllingPassenger());
        assertTrue(vehicle.isControlling(passenger));
        assertThrows(UnsupportedOperationException.class, snapshot::clear);

        vehicle.passengers.clear();

        assertEquals(List.of(passenger), snapshot);
        assertTrue(vehicle.getPassengers().isEmpty());
        assertNull(vehicle.getControllingPassenger());
        assertFalse(vehicle.isControlling(passenger));
    }

    @Test
    void skipsPassengersDetachedDuringAnotherPassengersUpdate() {
        CloudEntity first = entity();
        CloudEntity second = entity();
        ArrayList<Entity> moved = new ArrayList<>();

        CloudEntity vehicle = new CloudEntity(EntityTypes.PIG, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class))) {
            @Override
            protected void updatePassengerPosition(Entity passenger) {
                moved.add(passenger);
                second.vehicle = null;
                this.passengers.remove(second);
            }
        };

        first.vehicle = vehicle;
        second.vehicle = vehicle;
        vehicle.passengers.add(first);
        vehicle.passengers.add(second);
        vehicle.updatePassengers();

        assertEquals(java.util.List.of(first), moved);
        assertNull(second.getVehicle());
    }

    @Test
    void ejectionBlocksImmediateGameplayBoardingButNotExplicitMountRequests() {
        CloudEntity vehicle = entity();
        CloudEntity passenger = new CloudEntity(EntityTypes.PIG, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class))) {
            @Override
            public boolean mount(Entity target) {
                return true;
            }

            @Override
            protected void broadcastLinkPacket(Entity target, EntityLinkData.Type type, boolean riderInitiated) {
            }

            @Override
            public void flushEntityData() {
            }
        };
        passenger.vehicle = vehicle;
        vehicle.passengers.add(passenger);

        vehicle.ejectPassengers();
        assertNull(passenger.getVehicle());
        assertTrue(vehicle.getPassengers().isEmpty());
        assertFalse(passenger.tryMount(vehicle));
        assertTrue(passenger.mount(vehicle));
    }

    private static CloudEntity entity() {
        return new CloudEntity(EntityTypes.PIG,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class))) {
        };
    }
}
