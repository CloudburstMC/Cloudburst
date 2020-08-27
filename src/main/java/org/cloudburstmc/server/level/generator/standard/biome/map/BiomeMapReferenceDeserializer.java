package org.cloudburstmc.server.level.generator.standard.biome.map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.cloudburstmc.server.Nukkit;
import org.cloudburstmc.server.level.generator.standard.StandardGeneratorUtils;
import org.cloudburstmc.server.utils.Identifier;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author DaPorkchop_
 */
public final class BiomeMapReferenceDeserializer extends JsonDeserializer<BiomeMap> {
    @Override
    public BiomeMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        Identifier id = Identifier.fromString(p.getText());

        try (InputStream in = StandardGeneratorUtils.read("biomemap", id)) {
            return Nukkit.YAML_MAPPER.readValue(in, BiomeMap.class);
        } catch (IOException e) {
            throw new RuntimeException("While decoding biome map " + id, e);
        }
    }
}
