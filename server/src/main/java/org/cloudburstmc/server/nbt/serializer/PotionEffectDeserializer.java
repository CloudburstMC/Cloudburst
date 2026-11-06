package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.internal.NbtParser;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.potion.PotionEffectDataSerializer;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

public final class PotionEffectDeserializer extends StdDeserializer<PotionEffect> {

    public PotionEffectDeserializer() {
        super(PotionEffect.class);
    }

    @Override
    public PotionEffect deserialize(JsonParser p, DeserializationContext ctxt) {
        Object tag = p instanceof NbtParser parser ? parser.currentTag() : null;
        if (!(tag instanceof NbtMap map)) {
            MismatchedInputException mismatch = MismatchedInputException.from(p, PotionEffect.class,
                    "Cannot read a potion effect from " + NbtTags.describe(tag) + "; expected a compound");
            mismatch.initCause(new NbtKindMismatchException("Potion effects are stored as compounds"));
            throw mismatch;
        }
        p.skipChildren();
        return PotionEffectDataSerializer.deserialize(map);
    }
}
