package org.cloudburstmc.server.network.nethernet;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.netty.signaling.ServerStatus;
import org.cloudburstmc.server.network.CloudServerStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CloudExternalSignalingTest {

    @Test
    void mapsServerStatusWithinProviderLimits() {
        CloudServerStatus snapshot = new CloudServerStatus(
                Component.text("\n"), "world\n" + "x".repeat(128), GameMode.SPECTATOR,
                1, 20, false, true);

        ServerStatus listing = CloudExternalSignaling.listing(snapshot);
        assertEquals("Cloudburst", listing.name());
        assertEquals("world" + "x".repeat(123), listing.level());
        assertEquals(20, listing.maxPlayerCount());
        assertEquals(2, listing.gameType());
    }
}
