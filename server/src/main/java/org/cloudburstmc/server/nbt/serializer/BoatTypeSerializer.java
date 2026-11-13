package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.entity.vehicle.BoatType;
import org.cloudburstmc.server.entity.vehicle.VanillaBoats;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class BoatTypeSerializer extends StdSerializer<BoatType> {

    public BoatTypeSerializer() {
        super(BoatType.class);
    }

    @Override
    public void serialize(BoatType value, JsonGenerator gen, SerializationContext provider) {
        gen.writeNumber(VanillaBoats.definition(value).variant());
    }
}
