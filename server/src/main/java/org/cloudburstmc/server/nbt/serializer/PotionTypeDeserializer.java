package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.internal.NbtParser;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.server.network.NetworkUtils;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

public final class PotionTypeDeserializer extends StdDeserializer<PotionType> {

    public PotionTypeDeserializer() {
        super(PotionType.class);
    }

    @Override
    public PotionType deserialize(JsonParser p, DeserializationContext ctxt) {
        Object tag = p instanceof NbtParser parser ? parser.currentTag() : null;
        if (!(tag instanceof Number number)) {
            MismatchedInputException mismatch = MismatchedInputException.from(p, PotionType.class,
                    "Cannot read a potion type from " + NbtTags.describe(tag) + "; expected a short potion id");
            mismatch.initCause(new NbtKindMismatchException("Potion types are stored as short potion ids"));
            throw mismatch;
        }
        p.skipChildren();
        return NetworkUtils.potionFromNetwork(number.shortValue());
    }
}
