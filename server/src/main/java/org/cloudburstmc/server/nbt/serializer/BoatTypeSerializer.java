package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.entity.vehicle.BoatType;
import org.cloudburstmc.server.entity.vehicle.VanillaBoats;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Stores a boat hull as its variant int, as boats are stored today.
 * An id the variant mapping does not know fails the read with the mapping's own
 * error, exactly like the previous load path.
 */
public final class BoatTypeSerializer extends StdSerializer<BoatType> {

    public BoatTypeSerializer() {
        super(BoatType.class);
    }

    @Override
    public void serialize(BoatType value, JsonGenerator gen, SerializationContext provider) {
        gen.writeNumber(VanillaBoats.definition(value).variant());
    }
}
