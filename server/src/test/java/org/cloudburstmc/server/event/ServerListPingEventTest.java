package org.cloudburstmc.server.event;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.event.server.ServerListPingEvent;
import org.cloudburstmc.api.player.GameMode;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class ServerListPingEventTest {

    @Test
    void exposesRequesterContextUnderAnAsynchronousContract() {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 12345);
        ServerListPingEvent event = new ServerListPingEvent("example.org:19132", address,
                Component.text("Status"), "world", GameMode.SURVIVAL, 2, 10, false);
        assertTrue(event.isAsynchronous());
        assertEquals("example.org:19132", event.getHostname());
        assertEquals(address, event.getAddress());
    }

    @Test
    void supportsSuppressingTheStatusResponse() {
        ServerListPingEvent event = newEvent();
        assertFalse(event.isCancelled());
        event.setCancelled(true);
        assertTrue(event.isCancelled());
        event.setCancelled(false);
        assertFalse(event.isCancelled());
    }

    @Test
    void allowsAdvertisedCountsAboveTheAdvertisedMaximum() {
        ServerListPingEvent event = newEvent();
        event.setPlayerCount(30);
        event.setMaxPlayerCount(5);
        assertEquals(30, event.getPlayerCount());
        assertEquals(5, event.getMaxPlayerCount());
    }

    @Test
    void rejectsNegativeCountsWithoutChangingStatus() {
        ServerListPingEvent event = newEvent();
        assertThrows(IllegalArgumentException.class, () -> event.setPlayerCount(-1));
        assertThrows(IllegalArgumentException.class, () -> event.setMaxPlayerCount(-1));
        assertEquals(2, event.getPlayerCount());
        assertEquals(10, event.getMaxPlayerCount());
    }

    @Test
    void acceptsChangesToPresentationFields() {
        ServerListPingEvent event = newEvent();
        event.motd(Component.text("Changed"));
        event.setLevelName("other");
        event.setGameMode(GameMode.CREATIVE);
        event.setHardcore(true);
        assertEquals(Component.text("Changed"), event.motd());
        assertEquals("other", event.getLevelName());
        assertEquals(GameMode.CREATIVE, event.getGameMode());
        assertTrue(event.isHardcore());
    }

    @Test
    void rejectsNullPresentationFields() {
        ServerListPingEvent event = newEvent();
        assertThrows(NullPointerException.class, () -> event.motd(null));
        assertThrows(NullPointerException.class, () -> event.setLevelName(null));
        assertThrows(NullPointerException.class, () -> event.setGameMode(null));
    }

    private static ServerListPingEvent newEvent() {
        return new ServerListPingEvent("localhost", new InetSocketAddress("127.0.0.1", 12345),
                Component.text("Status"), "world", GameMode.SURVIVAL, 2, 10, false);
    }
}
