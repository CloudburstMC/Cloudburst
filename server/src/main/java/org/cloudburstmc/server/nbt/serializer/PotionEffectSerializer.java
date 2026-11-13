package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.server.potion.PotionEffectDataSerializer;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class PotionEffectSerializer extends StdSerializer<PotionEffect> {

    public PotionEffectSerializer() {
        super(PotionEffect.class);
    }

    @Override
    public void serialize(PotionEffect value, JsonGenerator gen, SerializationContext provider) {
        gen.writeEmbeddedObject(PotionEffectDataSerializer.serialize(value));
    }
}
