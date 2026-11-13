package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.server.network.NetworkUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class PotionTypeSerializer extends StdSerializer<PotionType> {

    public PotionTypeSerializer() {
        super(PotionType.class);
    }

    @Override
    public void serialize(PotionType value, JsonGenerator gen, SerializationContext provider) {
        gen.writeNumber(NetworkUtils.potionToNetwork(value));
    }
}
