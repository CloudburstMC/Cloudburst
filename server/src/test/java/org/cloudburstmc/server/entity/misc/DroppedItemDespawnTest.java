package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.event.entity.ItemDespawnEvent;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DroppedItemDespawnTest {

    @Test
    void emptyItemIsDiscardedWithoutAnExpiryEvent() {
        EntityDroppedItem item = (EntityDroppedItem) PersistenceFixtures.entity(EntityTypes.ITEM);

        assertFalse(item.tickEntity(1, 1));
        assertTrue(item.isClosed());
    }

    @Test
    void eventReportsTheLocationAtExpiry() {
        Level level = InterfaceProxy.create(Level.class);
        Location expiryLocation = Location.from(level);
        Location currentLocation = Location.from(1, 2, 3, level);
        DroppedItem item = InterfaceProxy.create(DroppedItem.class, Map.of("getLocation", currentLocation));

        ItemDespawnEvent event = new ItemDespawnEvent(item, expiryLocation);

        assertSame(item, event.getEntity());
        assertEquals(expiryLocation, event.getLocation());
    }
}
