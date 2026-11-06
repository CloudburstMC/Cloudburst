package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.internal.NbtParser;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.server.network.NetworkUtils;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

public final class EffectTypeDeserializer extends StdDeserializer<EffectType> {

    public EffectTypeDeserializer() {
        super(EffectType.class);
    }

    @Override
    public EffectType deserialize(JsonParser p, DeserializationContext ctxt) {
        Object tag = p instanceof NbtParser parser ? parser.currentTag() : null;
        if (!(tag instanceof Number number)) {
            MismatchedInputException mismatch = MismatchedInputException.from(p, EffectType.class,
                    "Cannot read an effect type from " + NbtTags.describe(tag) + "; expected an int effect id");
            mismatch.initCause(new NbtKindMismatchException("Effect types are stored as int effect ids"));
            throw mismatch;
        }
        p.skipChildren();
        return NetworkUtils.effectFromNetwork(number.byteValue());
    }
}
