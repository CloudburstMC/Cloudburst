package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.CreatureSpawnEvent;
import org.cloudburstmc.api.event.entity.CreatureSpawnReason;
import org.cloudburstmc.api.event.entity.EntitySpawnEvent;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.misc.EntityArmorStand;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EntitySpawnEventTest {

    @Test
    void armorStandUsesCreatureSpawnEventByDefault() {
        AtomicReference<EntitySpawnEvent> dispatched = new AtomicReference<>();
        Location location = Location.from(InterfaceProxy.create(Level.class));
        EntityArmorStand stand = new EntityArmorStand(EntityTypes.ARMOR_STAND, location) {
            @Override
            public boolean spawn(EntitySpawnEvent event) {
                dispatched.set(event);
                return false;
            }
        };

        assertFalse(stand.spawn());

        CreatureSpawnEvent event = assertInstanceOf(CreatureSpawnEvent.class, dispatched.get());
        assertSame(stand, event.getEntity());
        assertEquals(CreatureSpawnReason.DEFAULT, event.getSpawnReason());
    }

}
