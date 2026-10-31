package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.entity.vehicle.BoatType;
import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.internal.NbtParser;
import org.cloudburstmc.server.entity.vehicle.VanillaBoats;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

public final class BoatTypeDeserializer extends StdDeserializer<BoatType> {

    public BoatTypeDeserializer() {
        super(BoatType.class);
    }

    @Override
    public BoatType deserialize(JsonParser p, DeserializationContext ctxt) {
        Object tag = p instanceof NbtParser parser ? parser.currentTag() : null;
        if (!(tag instanceof Number number)) {
            MismatchedInputException mismatch = MismatchedInputException.from(p, BoatType.class,
                    "Cannot read a boat type from " + NbtTags.describe(tag) + "; expected an int variant");
            mismatch.initCause(new NbtKindMismatchException("Boat types are stored as int variants"));
            throw mismatch;
        }
        p.skipChildren();
        return VanillaBoats.fromVariant(number.intValue()).type();
    }
}
