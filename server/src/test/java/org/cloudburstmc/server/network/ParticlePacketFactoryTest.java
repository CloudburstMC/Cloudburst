package org.cloudburstmc.server.network;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.particle.*;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.SpawnParticleEffectPacket;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.awt.*;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParticlePacketFactoryTest {

    @Test
    void keepsNumberedEffectsSeparateFromNamedEmitters() {
        LevelEventPacket builtIn = ParticlePacketFactory.builtIn(ParticleTypes.CRIT, Vector3f.ZERO);
        assertSame(org.cloudburstmc.protocol.bedrock.data.ParticleType.CRIT, builtIn.getType());
        assertEquals(2, builtIn.getData());

        SpawnParticleEffectPacket emitter = ParticlePacketFactory.emitter(new ParticleEmitter(Identifier.parse("plugin:spark")), Vector3f.ZERO, 0);
        assertEquals("plugin:spark", emitter.getIdentifier());
        assertEquals(-1, emitter.getUniqueEntityId());
        assertTrue(emitter.getMolangVariablesJson().isEmpty());
    }

    @Test
    void encodesColorsAndRejectsMissingOrMismatchedValues() {
        Color color = new Color(12, 34, 56, 78);
        LevelEventPacket colored = ParticlePacketFactory.builtIn(new ColoredParticleOptions(ParticleTypes.FALLING_DUST, color), Vector3f.ZERO);
        assertEquals(color.getRGB(), colored.getData());
        assertEquals(7, ParticlePacketFactory.builtIn(new ScaledParticleOptions(ParticleTypes.CRIT, 7), Vector3f.ZERO).getData());
        assertThrows(IllegalArgumentException.class, () -> ParticlePacketFactory.builtIn(ParticleTypes.ICON_CRACK, Vector3f.ZERO));
        assertThrows(IllegalArgumentException.class, () -> ParticlePacketFactory.builtIn(new ColoredParticleOptions(ParticleTypes.CRIT, color), Vector3f.ZERO));
        assertThrows(IllegalArgumentException.class, () -> ParticlePacketFactory.builtIn(new ScaledParticleOptions(ParticleTypes.PORTAL, 2), Vector3f.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new ItemParticleOptions(ItemStack.EMPTY));
    }

    @Test
    void serializesAttachedEmittersWithStructuredVariables() {
        Entity attachment = (Entity) Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                new Class<?>[]{Entity.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> 42L;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "Particle attachment";
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        Map<String, ParticleVariable> members = new HashMap<>();
        members.put("r", new ParticleNumber(0.5f));

        ParticleStruct color = new ParticleStruct(members);
        members.put("g", new ParticleNumber(1));

        ParticleEmitter emitter = new ParticleEmitter(
                ParticleEmitterTypes.SPARKLER_EMITTER,
                attachment,
                Map.of("variable.color", color, "variable.size", new ParticleNumber(2))
        );

        Vector3f offset = Vector3f.from(0, 1, 0);
        SpawnParticleEffectPacket packet = ParticlePacketFactory.emitter(emitter, offset, 2);
        assertEquals(42, packet.getUniqueEntityId());
        assertEquals(offset, packet.getPosition());
        assertEquals(2, packet.getDimensionId());

        JsonNode variables = new ObjectMapper()
                .readTree(packet.getMolangVariablesJson().orElseThrow());
        assertEquals("variable.color", variables.get(0).get("name").stringValue());
        assertEquals("member_array", variables.get(0).get("value").get("type").stringValue());

        JsonNode member = variables.get(0).get("value").get("value");
        assertEquals(1, member.size());
        assertEquals(".r", member.get(0).get("name").stringValue());
        assertEquals("float", member.get(0).get("value").get("type").stringValue());
    }

    @Test
    void rejectsInvalidEmitterVariablesPositionsAndSpreads() {
        assertThrows(IllegalArgumentException.class, () -> new ParticleEmitter(Identifier.EMPTY));
        assertThrows(IllegalArgumentException.class, () -> new ParticleNumber(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new ParticleStruct(Map.of(".r", new ParticleNumber(1))));
        assertThrows(IllegalArgumentException.class, () -> new ParticleEmitter(ParticleEmitterTypes.SPARKLER_EMITTER, null, Map.of("color", new ParticleNumber(1))));
        assertThrows(IllegalArgumentException.class, () -> new ParticleEmission(ParticleTypes.CRIT, 0, Vector3f.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new ParticleEmission(ParticleTypes.CRIT, 1, Vector3f.from(-1, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> ParticlePacketFactory.builtIn(ParticleTypes.CRIT, Vector3f.from(0, Float.POSITIVE_INFINITY, 0)));
    }
}
