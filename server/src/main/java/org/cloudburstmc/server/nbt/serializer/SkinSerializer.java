package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.server.nbt.SkinNbt;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Stores a human skin as today's {@code Skin} compound, quirks included; see
 * {@link SkinNbt}.
 */
public final class SkinSerializer extends StdSerializer<Skin> {

    public SkinSerializer() {
        super(Skin.class);
    }

    @Override
    public void serialize(Skin value, JsonGenerator gen, SerializationContext provider) {
        gen.writeEmbeddedObject(SkinNbt.toNbt(value));
    }
}
