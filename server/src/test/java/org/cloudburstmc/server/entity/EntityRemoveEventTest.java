package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.event.entity.EntityRemoveEvent;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.misc.EntityExperienceOrb;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityRemoveEventTest {

    @Test
    void eventReportsTheEntityAndItsRemovalCause() {
        Entity entity = InterfaceProxy.create(Entity.class);

        EntityRemoveEvent event = new EntityRemoveEvent(entity, EntityRemoveCause.UNLOAD);

        assertSame(entity, event.getEntity());
        assertEquals(EntityRemoveCause.UNLOAD, event.getCause());
    }

    @Test
    void closingThroughTheApiIsAPluginRemoval() {
        TrackingOrb orb = new TrackingOrb();

        orb.close();

        assertTrue(orb.isClosed());
        assertEquals(EntityRemoveCause.PLUGIN, orb.cause);
    }

    @Test
    void onlyTheLastPickupRemovesAStackedOrb() {
        TrackingOrb orb = new TrackingOrb();
        orb.setCount(2);

        orb.consumeOne();
        assertFalse(orb.isClosed());
        assertNull(orb.cause);

        orb.consumeOne();
        assertTrue(orb.isClosed());
        assertEquals(EntityRemoveCause.PICKUP, orb.cause);
    }

    static class TrackingOrb extends EntityExperienceOrb {

        private EntityRemoveCause cause;

        TrackingOrb() {
            super(EntityTypes.XP_ORB, Location.from(InterfaceProxy.create(Level.class)));
        }

        @Override
        public void remove(EntityRemoveCause cause) {
            super.remove(cause);
            this.cause = cause;
        }
    }
}
