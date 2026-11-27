package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerKickEventTest {

    @Test
    void causeAndReasonAreIndependentOfTheLeaveMessage() {
        Player player = InterfaceProxy.create(Player.class);
        Component leaveMessage = Component.text("Player left");
        Component reason = Component.text("Address banned");
        PlayerKickEvent event = new PlayerKickEvent(player, reason, leaveMessage, KickCause.IP_BANNED);

        assertSame(player, event.getPlayer());
        assertEquals(KickCause.IP_BANNED, event.getCause());
        assertEquals(reason, event.reason());
        assertEquals(leaveMessage, event.leaveMessage());

        event.leaveMessage(null);

        assertNull(event.leaveMessage());
        assertEquals(KickCause.IP_BANNED, event.getCause());
        assertEquals(reason, event.reason());
    }

    @Test
    void reasonCanBeReplaced() {
        PlayerKickEvent event = new PlayerKickEvent(InterfaceProxy.create(Player.class), Component.text("a"), null, KickCause.UNKNOWN);

        event.reason(Component.text("b"));

        assertEquals(Component.text("b"), event.reason());
    }
}
