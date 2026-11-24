package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.EntityTeleportEvent;
import org.cloudburstmc.api.event.entity.TeleportCause;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EntityTeleportEventTest {

    @Test
    void recursiveTeleportDuringDestinationEventIsRejected() {
        Location location = Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class));
        AtomicInteger requests = new AtomicInteger();
        CloudEntity entity = new CloudEntity(EntityTypes.PIG, location) {
            @Override
            public Location getLocation() {
                return location;
            }

            @Override
            protected boolean isTeleportUnavailable(Location target) {
                return false;
            }

            @Override
            protected Location requestTeleportDestination(Location from, Location to, TeleportCause cause) {
                requests.incrementAndGet();
                assertFalse(this.teleport(to, cause));
                assertFalse(this.teleportWithoutEvent(to));
                return null;
            }
        };

        assertFalse(entity.teleport(location, TeleportCause.PLUGIN));
        assertEquals(1, requests.get());
    }

    @Test
    void destinationChangeRetainsCauseAndOrigin() {
        Entity entity = InterfaceProxy.create(Entity.class);
        Level level = InterfaceProxy.create(Level.class);
        Location from = Location.from(Vector3f.ZERO, level);
        Location to = Location.from(Vector3f.from(1, 2, 3), level);
        EntityTeleportEvent event = new EntityTeleportEvent(entity, from, to, TeleportCause.NETHER_PORTAL);

        Location replacement = Location.from(Vector3f.from(4, 5, 6), level);
        event.setTo(replacement);

        assertSame(entity, event.getEntity());
        assertSame(from, event.getFrom());
        assertSame(replacement, event.getTo());
        assertEquals(TeleportCause.NETHER_PORTAL, event.getCause());
    }
}
