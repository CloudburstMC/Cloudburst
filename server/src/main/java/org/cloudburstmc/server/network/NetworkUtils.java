package org.cloudburstmc.server.network;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.Attribute;
import org.cloudburstmc.api.level.gamerule.LevelGameRules;
import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket;
import org.cloudburstmc.server.registry.CloudParticleRegistry;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

/**
 * Utility class providing static helpers for protocol conversions: potion and effect type ↔ protocol ID,
 * attribute and game-rule serialization, and address formatting.
 */
@UtilityClass
public class NetworkUtils {

    private final BiMap<PotionType, Short> potionTypeMap = HashBiMap.create();
    private final BiMap<EffectType, Byte> effectTypeMap = HashBiMap.create();
    private final Map<ParticleType, org.cloudburstmc.protocol.bedrock.data.ParticleType> particleTypeMap = createParticleTypeMap();

    static {
        potionTypeMap.put(PotionTypes.WATER, (short) 0);
        potionTypeMap.put(PotionTypes.MUNDANE, (short) 1);
        potionTypeMap.put(PotionTypes.LONG_MUNDANE, (short) 2);
        potionTypeMap.put(PotionTypes.THICK, (short) 3);
        potionTypeMap.put(PotionTypes.AWKWARD, (short) 4);
        potionTypeMap.put(PotionTypes.NIGHT_VISION, (short) 5);
        potionTypeMap.put(PotionTypes.LONG_NIGHT_VISION, (short) 6);
        potionTypeMap.put(PotionTypes.INVISIBILITY, (short) 7);
        potionTypeMap.put(PotionTypes.LONG_INVISIBILITY, (short) 8);
        potionTypeMap.put(PotionTypes.LEAPING, (short) 9);
        potionTypeMap.put(PotionTypes.LONG_LEAPING, (short) 10);
        potionTypeMap.put(PotionTypes.STRONG_LEAPING, (short) 11);
        potionTypeMap.put(PotionTypes.FIRE_RESISTANCE, (short) 12);
        potionTypeMap.put(PotionTypes.LONG_FIRE_RESISTANCE, (short) 13);
        potionTypeMap.put(PotionTypes.SWIFTNESS, (short) 14);
        potionTypeMap.put(PotionTypes.LONG_SWIFTNESS, (short) 15);
        potionTypeMap.put(PotionTypes.STRONG_SWIFTNESS, (short) 16);
        potionTypeMap.put(PotionTypes.SLOWNESS, (short) 17);
        potionTypeMap.put(PotionTypes.LONG_SLOWNESS, (short) 18);
        potionTypeMap.put(PotionTypes.WATER_BREATHING, (short) 19);
        potionTypeMap.put(PotionTypes.LONG_WATER_BREATHING, (short) 20);
        potionTypeMap.put(PotionTypes.HEALING, (short) 21);
        potionTypeMap.put(PotionTypes.STRONG_HEALING, (short) 22);
        potionTypeMap.put(PotionTypes.HARMING, (short) 23);
        potionTypeMap.put(PotionTypes.STRONG_HARMING, (short) 24);
        potionTypeMap.put(PotionTypes.POISON, (short) 25);
        potionTypeMap.put(PotionTypes.LONG_POISON, (short) 26);
        potionTypeMap.put(PotionTypes.STRONG_POISON, (short) 27);
        potionTypeMap.put(PotionTypes.REGENERATION, (short) 28);
        potionTypeMap.put(PotionTypes.LONG_REGENERATION, (short) 29);
        potionTypeMap.put(PotionTypes.STRONG_REGENERATION, (short) 30);
        potionTypeMap.put(PotionTypes.STRENGTH, (short) 31);
        potionTypeMap.put(PotionTypes.LONG_STRENGTH, (short) 32);
        potionTypeMap.put(PotionTypes.STRONG_STRENGTH, (short) 33);
        potionTypeMap.put(PotionTypes.WEAKNESS, (short) 34);
        potionTypeMap.put(PotionTypes.LONG_WEAKNESS, (short) 35);
        potionTypeMap.put(PotionTypes.WITHER, (short) 36);
        potionTypeMap.put(PotionTypes.TURTLE_MASTER, (short) 37);
        potionTypeMap.put(PotionTypes.LONG_TURTLE_MASTER, (short) 38);
        potionTypeMap.put(PotionTypes.STRONG_TURTLE_MASTER, (short) 39);
        potionTypeMap.put(PotionTypes.SLOW_FALLING, (short) 40);
        potionTypeMap.put(PotionTypes.LONG_SLOW_FALLING, (short) 41);
        potionTypeMap.put(PotionTypes.STRONG_SLOWNESS, (short) 42);
        potionTypeMap.put(PotionTypes.WIND_CHARGED, (short) 43);
        potionTypeMap.put(PotionTypes.WEAVING, (short) 44);
        potionTypeMap.put(PotionTypes.OOZING, (short) 45);
        potionTypeMap.put(PotionTypes.INFESTED, (short) 46);

        effectTypeMap.put(EffectTypes.SPEED, (byte) 1);
        effectTypeMap.put(EffectTypes.SLOWNESS, (byte) 2);
        effectTypeMap.put(EffectTypes.HASTE, (byte) 3);
        effectTypeMap.put(EffectTypes.MINING_FATIGUE, (byte) 4);
        effectTypeMap.put(EffectTypes.STRENGTH, (byte) 5);
        effectTypeMap.put(EffectTypes.INSTANT_HEALTH, (byte) 6);
        effectTypeMap.put(EffectTypes.INSTANT_DAMAGE, (byte) 7);
        effectTypeMap.put(EffectTypes.JUMP_BOOST, (byte) 8);
        effectTypeMap.put(EffectTypes.NAUSEA, (byte) 9);
        effectTypeMap.put(EffectTypes.REGENERATION, (byte) 10);
        effectTypeMap.put(EffectTypes.RESISTANCE, (byte) 11);
        effectTypeMap.put(EffectTypes.FIRE_RESISTANCE, (byte) 12);
        effectTypeMap.put(EffectTypes.WATER_BREATHING, (byte) 13);
        effectTypeMap.put(EffectTypes.INVISIBILITY, (byte) 14);
        effectTypeMap.put(EffectTypes.BLINDNESS, (byte) 15);
        effectTypeMap.put(EffectTypes.NIGHT_VISION, (byte) 16);
        effectTypeMap.put(EffectTypes.HUNGER, (byte) 17);
        effectTypeMap.put(EffectTypes.WEAKNESS, (byte) 18);
        effectTypeMap.put(EffectTypes.POISON, (byte) 19);
        effectTypeMap.put(EffectTypes.WITHER, (byte) 20);
        effectTypeMap.put(EffectTypes.HEALTH_BOOST, (byte) 21);
        effectTypeMap.put(EffectTypes.ABSORPTION, (byte) 22);
        effectTypeMap.put(EffectTypes.SATURATION, (byte) 23);
        effectTypeMap.put(EffectTypes.LEVITATION, (byte) 24);
        effectTypeMap.put(EffectTypes.FATAL_POISON, (byte) 25);
        effectTypeMap.put(EffectTypes.CONDUIT_POWER, (byte) 26);
        effectTypeMap.put(EffectTypes.SLOW_FALLING, (byte) 27);
        effectTypeMap.put(EffectTypes.BAD_OMEN, (byte) 28);
        effectTypeMap.put(EffectTypes.VILLAGE_HERO, (byte) 29);
        effectTypeMap.put(EffectTypes.DARKNESS, (byte) 30);
        effectTypeMap.put(EffectTypes.TRIAL_OMEN, (byte) 31);
        effectTypeMap.put(EffectTypes.WIND_CHARGED, (byte) 32);
        effectTypeMap.put(EffectTypes.WEAVING, (byte) 33);
        effectTypeMap.put(EffectTypes.OOZING, (byte) 34);
        effectTypeMap.put(EffectTypes.INFESTED, (byte) 35);
        effectTypeMap.put(EffectTypes.RAID_OMEN, (byte) 36);
        effectTypeMap.put(EffectTypes.BREATH_OF_THE_NAUTILUS, (byte) 37);
    }

    public static AttributeData attributeToNetwork(Attribute attr) {
        return new AttributeData(
                attr.getName(),
                attr.getMinValue(),
                attr.getMaxValue(),
                attr.getValue(),
                attr.getDefaultValue()
        );
    }

    public static void gameRulesToNetwork(LevelGameRules gameRules, List<GameRuleData<?>> networkRules) {
        for (LevelGameRules.Entry<?> entry : gameRules) {
            networkRules.add(new GameRuleData<>(entry.rule().getName(), entry.value()));
        }
    }

    public static short potionToNetwork(PotionType type) {
        Objects.requireNonNull(type, "type");
        Short networkId = potionTypeMap.get(type);
        if (networkId == null) {
            throw new IllegalArgumentException("Potion type is not available on the network: " + type.getId());
        }

        return networkId;
    }

    public static PotionType potionFromNetwork(short potionId) {
        PotionType type = potionTypeMap.inverse().get(potionId);
        if (type == null) {
            throw new IllegalArgumentException("Unknown network potion ID: " + potionId);
        }

        return type;
    }

    public static byte effectToNetwork(EffectType type) {
        Objects.requireNonNull(type, "type");
        Byte networkId = effectTypeMap.get(type);
        if (networkId == null) {
            throw new IllegalArgumentException("Effect type is not available on the network: " + type.getId());
        }

        return networkId;
    }

    public static EffectType effectFromNetwork(byte effectId) {
        EffectType type = effectTypeMap.inverse().get(effectId);
        if (type == null) {
            throw new IllegalArgumentException("Unknown network effect ID: " + effectId);
        }

        return type;
    }

    public static MobEffectPacket effectToNetwork(PotionEffect effect, long runtimeEntityId, MobEffectPacket.Event event, long tick) {
        MobEffectPacket packet = new MobEffectPacket();
        packet.setRuntimeEntityId(runtimeEntityId);
        packet.setEvent(event);
        packet.setEffectId(effectToNetwork(effect.getType()));
        packet.setAmplifier(effect.getAmplifier());
        packet.setParticles(effect.hasParticles());
        packet.setDuration(effect.getDuration());
        packet.setTick(tick);
        packet.setAmbient(effect.isAmbient());
        return packet;
    }

    public static org.cloudburstmc.protocol.bedrock.data.ParticleType particleToNetwork(ParticleType type) {
        Objects.requireNonNull(type, "type");
        return particleTypeMap.getOrDefault(type, org.cloudburstmc.protocol.bedrock.data.ParticleType.UNDEFINED);
    }

    public static ParticleType particleFromNetwork(org.cloudburstmc.protocol.bedrock.data.ParticleType type) {
        Objects.requireNonNull(type, "type");
        Identifier id = Identifier.parse(type.name().toLowerCase(Locale.ROOT));
        return CloudParticleRegistry.get().get(id).orElse(ParticleTypes.UNDEFINED);
    }

    private static Map<ParticleType, org.cloudburstmc.protocol.bedrock.data.ParticleType> createParticleTypeMap() {
        Map<ParticleType, org.cloudburstmc.protocol.bedrock.data.ParticleType> values = new HashMap<>();
        for (org.cloudburstmc.protocol.bedrock.data.ParticleType type :
                org.cloudburstmc.protocol.bedrock.data.ParticleType.values()) {
            Identifier id = Identifier.parse(type.name().toLowerCase(Locale.ROOT));
            CloudParticleRegistry.get().get(id).ifPresent(apiType -> values.put(apiType, type));
        }
        return Map.copyOf(values);
    }

    public static String loggableAddress(SocketAddress address, boolean logAddress) {
        if (!logAddress) {
            return "<ip address withheld>";
        }

        if (address instanceof InetSocketAddress inetAddress) {
            return inetAddress.getHostString() + ":" + inetAddress.getPort();
        }

        return String.valueOf(address);
    }
}
