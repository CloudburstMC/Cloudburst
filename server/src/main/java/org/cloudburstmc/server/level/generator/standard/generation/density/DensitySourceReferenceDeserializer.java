package org.cloudburstmc.server.level.generator.standard.generation.density;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.level.generator.standard.StandardGeneratorUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author DaPorkchop_
 */
public final class DensitySourceReferenceDeserializer extends JsonDeserializer<DensitySource> {
    @Override
    public DensitySource deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        Identifier id = Identifier.parse(p.getText());

        try (InputStream in = StandardGeneratorUtils.read("density", id)) {
            return Bootstrap.YAML_MAPPER.readValue(in, DensitySource.class);
        } catch (IOException e) {
            throw new RuntimeException("While decoding density source " + id, e);
        }
    }
}
