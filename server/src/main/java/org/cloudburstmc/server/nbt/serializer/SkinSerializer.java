package org.cloudburstmc.server.nbt.serializer;

import org.cloudburstmc.api.player.skin.Skin;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class SkinSerializer extends StdSerializer<Skin> {

    public SkinSerializer() {
        super(Skin.class);
    }

    @Override
    public void serialize(Skin value, JsonGenerator gen, SerializationContext provider) {
        gen.writeEmbeddedObject(SkinNbt.toNbt(value));
    }
}
