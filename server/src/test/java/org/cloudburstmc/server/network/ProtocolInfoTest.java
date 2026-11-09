package org.cloudburstmc.server.network;

import org.cloudburstmc.protocol.bedrock.codec.v2225.Bedrock_v2225;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProtocolInfoTest {

    @Test
    void usesTheCurrentProtocolByDefault() {
        assertSame(Bedrock_v2225.CODEC, ProtocolInfo.getDefaultPacketCodec());
    }

    @Test
    void advertisesTheCurrentGameVersion() {
        assertEquals("1.26.60", ProtocolInfo.getDefaultMinecraftVersion());
    }

    @Test
    void acceptsTheAdvertisedProtocol() {
        assertSame(Bedrock_v2225.CODEC, ProtocolInfo.getPacketCodec(2225));
    }
}
