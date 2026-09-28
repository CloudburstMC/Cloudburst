package org.cloudburstmc.server.network;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.particle.*;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.SpawnParticleEffectPacket;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Encodes particle rendering values without exposing their packed representation.
 */
@UtilityClass
public class ParticlePacketFactory {
    private static final ObjectMapper JSON = new ObjectMapper();

    public static LevelEventPacket builtIn(ParticleOptions options, Vector3f position) {
        Objects.requireNonNull(options, "options");
        validatePosition(position);
        org.cloudburstmc.protocol.bedrock.data.ParticleType type = NetworkUtils.particleToNetwork(options.getType());
        int data = switch (options) {
            case BlockParticleOptions block -> switch (type) {
                case TERRAIN, FALLING_DUST -> CloudBlockRegistry.REGISTRY.getRuntimeId(block.state());
                default -> throw unsupported(options);
            };
            case ItemParticleOptions item -> {
                ItemData networkItem = ItemUtils.toNetwork(item.item());
                yield (networkItem.getDefinition().getRuntimeId() << 16) | (networkItem.getDamage() & 0xffff);
            }
            case ColoredParticleOptions colored -> switch (type) {
                case FALLING_DUST, MOB_SPELL, MOB_SPELL_AMBIENT, MOB_SPELL_INSTANTANEOUS -> colored.color().getRGB();
                default -> throw unsupported(options);
            };
            case ScaledParticleOptions scaled -> switch (type) {
                case CRIT, SMOKE, HEART, INK -> scaled.scale();
                default -> throw unsupported(options);
            };
            case ParticleType ignored -> switch (type) {
                case ICON_CRACK, TERRAIN, FALLING_DUST ->
                        throw new IllegalArgumentException("Particle requires rendering values: " + options.getType().getId());
                case CRIT -> 2;
                case RED_DUST -> 1;
                default -> 0;
            };
            default -> throw unsupported(options);
        };

        LevelEventPacket packet = new LevelEventPacket();
        packet.setType(type);
        packet.setPosition(position);
        packet.setData(data);
        return packet;
    }

    public static SpawnParticleEffectPacket emitter(ParticleEmitter emitter, Vector3f position, int dimension) {
        Objects.requireNonNull(emitter, "emitter");
        validatePosition(position);
        Entity attachment = emitter.attachment();
        SpawnParticleEffectPacket packet = new SpawnParticleEffectPacket();
        packet.setIdentifier(emitter.type().getId().toString());
        packet.setPosition(position);
        packet.setUniqueEntityId(attachment == null ? -1 : attachment.getUniqueId());
        packet.setDimensionId(dimension);
        packet.setMolangVariablesJson(emitter.variables().isEmpty() ? Optional.empty()
                : Optional.of(JSON.writeValueAsString(variables(emitter.variables(), ""))));
        return packet;
    }

    private static List<Map<String, Object>> variables(Map<String, ParticleVariable> variables, String prefix) {
        return variables.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> Map.of("name", prefix + entry.getKey(), "value", variable(entry.getValue())))
                .toList();
    }

    private static Map<String, Object> variable(ParticleVariable value) {
        return switch (value) {
            case ParticleNumber number -> Map.of("type", "float", "value", number.value());
            case ParticleStruct struct -> Map.of("type", "member_array", "value", variables(struct.members(), "."));
        };
    }

    private static IllegalArgumentException unsupported(ParticleOptions options) {
        return new IllegalArgumentException("Unsupported options for " + options.getType().getId() + ": "
                + options.getClass().getSimpleName());
    }

    public static void validatePosition(Vector3f position) {
        Objects.requireNonNull(position, "position");
        if (!Float.isFinite(position.getX()) || !Float.isFinite(position.getY()) || !Float.isFinite(position.getZ())) {
            throw new IllegalArgumentException("Particle position must be finite");
        }
    }
}
