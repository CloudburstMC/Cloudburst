package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.player.KickReason;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerKickEventTest {

    @Test
    void categoryAndDisconnectTextAreIndependentOfTheLeaveMessage() {
        Player player = InterfaceProxy.create(Player.class);
        Component leaveMessage = Component.text("Player left");
        PlayerKickEvent event = new PlayerKickEvent(player, KickReason.IP_BANNED, "Address banned", leaveMessage);

        assertSame(player, event.getPlayer());
        assertEquals(KickReason.IP_BANNED, event.getKickReason());
        assertEquals("Address banned", event.getReason());
        assertEquals(leaveMessage, event.getQuitMessage());

        event.setQuitMessage(null);

        assertNull(event.getQuitMessage());
        assertEquals(KickReason.IP_BANNED, event.getKickReason());
        assertEquals("Address banned", event.getReason());
    }

    @Test
    void omittedDisconnectTextUsesTheCategoryName() {
        PlayerKickEvent event = new PlayerKickEvent(InterfaceProxy.create(Player.class), KickReason.UNKNOWN, null);

        assertEquals(KickReason.UNKNOWN, event.getKickReason());
        assertEquals("UNKNOWN", event.getReason());
    }
}
