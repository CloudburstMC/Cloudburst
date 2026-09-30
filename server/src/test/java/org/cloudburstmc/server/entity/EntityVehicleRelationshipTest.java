package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.data.MountType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

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

        assertFalse(vehicle.mount(vehicle, MountType.RIDER));
        assertFalse(vehicle.mount(passenger, MountType.RIDER));
        assertNull(vehicle.getVehicle());
        assertSame(vehicle, passenger.getVehicle());
    }

    private static CloudEntity entity() {
        return new CloudEntity(EntityTypes.PIG,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class))) {
        };
    }
}
