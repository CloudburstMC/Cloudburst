package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.server.network.NetworkUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class EffectTypeSerializer extends StdSerializer<EffectType> {

    public EffectTypeSerializer() {
        super(EffectType.class);
    }

    @Override
    public void serialize(EffectType value, JsonGenerator gen, SerializationContext provider) {
        gen.writeNumber((int) NetworkUtils.effectToNetwork(value));
    }
}
