package org.cloudburstmc.server.potion;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.network.NetworkUtils;

import static java.util.Objects.requireNonNull;

/**
 * Serializes potion effects stored in entity data.
 */
@UtilityClass
public class PotionEffectDataSerializer {
    private static final String TAG_ID = "Id";
    private static final String TAG_AMPLIFIER = "Amplifier";
    private static final String TAG_DURATION = "Duration";
    private static final String TAG_AMBIENT = "Ambient";
    private static final String TAG_SHOW_PARTICLES = "ShowParticles";

    public PotionEffect deserialize(NbtMap tag) {
        requireNonNull(tag, "tag");
        return new PotionEffect(
                NetworkUtils.effectFromNetwork(tag.getByte(TAG_ID)),
                tag.getInt(TAG_DURATION),
                Byte.toUnsignedInt(tag.getByte(TAG_AMPLIFIER)),
                tag.getBoolean(TAG_AMBIENT),
                tag.getBoolean(TAG_SHOW_PARTICLES));
    }

    public NbtMap serialize(PotionEffect effect) {
        requireNonNull(effect, "effect");
        return NbtMap.builder()
                .putByte(TAG_ID, NetworkUtils.effectToNetwork(effect.getType()))
                .putByte(TAG_AMPLIFIER, (byte) effect.getAmplifier())
                .putInt(TAG_DURATION, effect.getDuration())
                .putBoolean(TAG_AMBIENT, effect.isAmbient())
                .putBoolean(TAG_SHOW_PARTICLES, effect.hasParticles())
                .build();
    }
}
