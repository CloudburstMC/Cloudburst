package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.server.potion.PotionEffectDataSerializer;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Stores a potion effect as its data compound, as area effect clouds are stored today.
 * An effect id the mapping does not know fails the read with the mapping's own error,
 * exactly like the previous load path.
 */
public final class PotionEffectSerializer extends StdSerializer<PotionEffect> {

    public PotionEffectSerializer() {
        super(PotionEffect.class);
    }

    @Override
    public void serialize(PotionEffect value, JsonGenerator gen, SerializationContext provider) {
        gen.writeEmbeddedObject(PotionEffectDataSerializer.serialize(value));
    }
}
