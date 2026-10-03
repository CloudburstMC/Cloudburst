package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.vehicle.Boat;
import org.cloudburstmc.api.event.vehicle.VehicleExitEvent;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleExitEventTest {

    @Test
    void cancellationCannotKeepAPassengerInARemovedVehicle() {
        Boat boat = InterfaceProxy.create(Boat.class);
        Entity passenger = InterfaceProxy.create(Entity.class);
        VehicleExitEvent voluntary = new VehicleExitEvent(boat, passenger);
        voluntary.setCancelled();
        assertTrue(voluntary.isCancelled());

        VehicleExitEvent forced = new VehicleExitEvent(boat, passenger, false);
        forced.setCancelled();
        assertFalse(forced.isCancellable());
        assertFalse(forced.isCancelled());
    }
}
