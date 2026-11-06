package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.server.network.NetworkUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Stores an effect type as its network id in an int, as beacon effects are stored today.
 * An id the mapping does not know fails the read with the mapping's own error, exactly
 * like the previous load path, which converted the stored int after reading it.
 */
public final class EffectTypeSerializer extends StdSerializer<EffectType> {

    public EffectTypeSerializer() {
        super(EffectType.class);
    }

    @Override
    public void serialize(EffectType value, JsonGenerator gen, SerializationContext provider) {
        gen.writeNumber((int) NetworkUtils.effectToNetwork(value));
    }
}
