package org.cloudburstmc.server.level.generator.standard.biome.map;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.level.generator.standard.StandardGeneratorUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author DaPorkchop_
 */
public final class BiomeMapReferenceDeserializer extends ValueDeserializer<BiomeMap> {
    @Override
    public BiomeMap deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        Identifier id = Identifier.parse(p.getText());

        try (InputStream in = StandardGeneratorUtils.read("biomemap", id)) {
            return Bootstrap.YAML_MAPPER.readValue(in, BiomeMap.class);
        } catch (IOException e) {
            throw new RuntimeException("While decoding biome map " + id, e);
        }
    }
}
