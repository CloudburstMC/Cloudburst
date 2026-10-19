package org.cloudburstmc.server.level.generator.standard.biome.map;

import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.StandardGeneratorUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.io.IOException;
import java.io.InputStream;

public final class BiomeMapReferenceDeserializer extends ValueDeserializer<BiomeMap> {

    @Override
    public BiomeMap deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        Identifier id = Identifier.parse(p.getString());

        try (InputStream in = StandardGeneratorUtils.read("biomemap", id)) {
            return StandardGeneratorUtils.decode(in, BiomeMap.class);
        } catch (IOException e) {
            throw new RuntimeException("While decoding biome map " + id, e);
        }
    }
}
