package org.cloudburstmc.server.network;

import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkEntityAttributeTest {

    @Test
    void healthUsesTheEntitiesEffectiveMaximum() {
        AttributeData player = NetworkEntityAttribute.encodeHealth(40f, 24);
        AttributeData dragon = NetworkEntityAttribute.encodeHealth(150f, 200);

        assertEquals("minecraft:health", player.getName());
        assertEquals(24f, player.getValue());
        assertEquals(24f, player.getMaximum());
        assertEquals(24f, player.getDefaultValue());
        assertEquals(150f, dragon.getValue());
        assertEquals(200f, dragon.getMaximum());
    }

    @Test
    void packetValuesAreBoundedWithoutChangingTheDefinition() {
        NetworkEntityAttribute experience = NetworkEntityAttribute.EXPERIENCE;

        assertEquals(0f, experience.encode(Float.NaN).getValue());
        assertEquals(0f, experience.encode(Float.NEGATIVE_INFINITY).getValue());
        assertEquals(1f, experience.encode(Float.POSITIVE_INFINITY).getValue());
        assertEquals(0.5f, experience.encode(0.5f).getValue());
        assertEquals(0f, experience.encode(0.5f).getDefaultValue());
    }
}
