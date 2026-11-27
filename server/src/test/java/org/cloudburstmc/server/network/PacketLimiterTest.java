package org.cloudburstmc.server.network;

import org.cloudburstmc.protocol.bedrock.packet.BookEditPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;
import org.cloudburstmc.server.configuration.GlobalConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PacketLimiterTest {

    @Test
    void listedPacketsShareOneAllowanceAndOthersAreUnlimited() {
        PacketLimiter limiter = PacketLimiter.create(List.of(GlobalConfiguration.PacketLimit.builder()
                .packets(List.of("TextPacket", "BookEditPacket"))
                .rate(0.001)
                .burst(2)
                .build()));

        assertTrue(limiter.tryAcquire(new TextPacket()));
        assertTrue(limiter.tryAcquire(new BookEditPacket()));
        assertFalse(limiter.tryAcquire(new TextPacket()));
        assertTrue(limiter.tryAcquire(new PlayerSkinPacket()));
    }

    @Test
    void packetTypesResolveBySimpleName() {
        assertEquals(TextPacket.class, PacketLimiter.packetType("TextPacket").orElseThrow());
        assertTrue(PacketLimiter.packetType("NotAPacket").isEmpty());
        assertTrue(PacketLimiter.packetType("BedrockPacket").isEmpty());
    }
}
