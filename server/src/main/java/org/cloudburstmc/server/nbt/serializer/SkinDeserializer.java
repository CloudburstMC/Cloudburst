package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.server.nbt.SkinNbt;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public final class SkinDeserializer extends StdDeserializer<Skin> {

    public SkinDeserializer() {
        super(Skin.class);
    }

    @Override
    public Skin deserialize(JsonParser p, DeserializationContext ctxt) {
        return SkinNbt.fromNbt(NbtTags.readTag(p, ctxt));
    }
}
