package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinecartActivationTest {

    @Test
    void hopperActivatorRailDisablesWhenPoweredAndEnablesWhenUnpowered() {
        EntityHopperMinecart cart = (EntityHopperMinecart) PersistenceFixtures.entity(EntityTypes.HOPPER_MINECART);
        assertTrue(cart.isEnabled());

        cart.activate(0, 0, 0, true);
        assertFalse(cart.isEnabled());

        cart.activate(0, 0, 0, false);
        assertTrue(cart.isEnabled());
    }

    @Test
    void unpoweredActivatorRailDoesNotPrimeTnt() {
        EntityTntMinecart cart = (EntityTntMinecart) PersistenceFixtures.entity(EntityTypes.TNT_MINECART);

        cart.activate(0, 0, 0, false);

        assertEquals(-1, cart.getFuseTicks());
    }

    @Test
    void poweredActivatorRailEjectsPassengersAndStartsHurtAnimation() {
        ActivationTarget cart = new ActivationTarget();

        cart.activate(0, 0, 0, false);
        assertEquals(0, cart.ejections);
        assertEquals(0, cart.getHurtTime());

        cart.activate(0, 0, 0, true);
        assertEquals(1, cart.ejections);
        assertEquals(10, cart.getHurtTime());
        assertEquals(-1, cart.getHurtDirection());
        assertEquals(50, cart.getDamage());

        cart.activate(0, 0, 0, true);
        assertEquals(2, cart.ejections);
        assertEquals(-1, cart.getHurtDirection());
    }

    private static class ActivationTarget extends EntityMinecart {
        private int ejections;

        private ActivationTarget() {
            super(EntityTypes.MINECART, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        }

        @Override
        protected void ejectPassengers() {
            this.ejections++;
        }
    }
}
