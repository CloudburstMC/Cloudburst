package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.vehicle.Boat;
import org.cloudburstmc.api.event.vehicle.VehicleDamageEvent;
import org.cloudburstmc.api.event.vehicle.VehicleDestroyEvent;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class VehicleDamageEventTest {

    @Test
    void exposesTheProjectileAsAttackerAndRetainsItsShooterInTheSource() {
        Boat boat = InterfaceProxy.create(Boat.class);
        Entity arrow = InterfaceProxy.create(Entity.class);
        Entity shooter = InterfaceProxy.create(Entity.class);
        DamageSource source = DamageSource.builder(DamageTypes.ARROW)
                .directEntity(arrow)
                .causingEntity(shooter)
                .build();

        VehicleDamageEvent damage = new VehicleDamageEvent(boat, source, 3);
        VehicleDestroyEvent destroy = new VehicleDestroyEvent(boat, source);

        assertSame(arrow, damage.getAttacker());
        assertSame(arrow, destroy.getAttacker());
        assertSame(shooter, damage.getDamageSource().getCausingEntity());
        assertSame(source, destroy.getDamageSource());
    }
}
