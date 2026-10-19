package org.cloudburstmc.server.level.generator.standard.generation.density;

import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.StandardGeneratorUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.io.IOException;
import java.io.InputStream;

public final class DensitySourceReferenceDeserializer extends ValueDeserializer<DensitySource> {

    @Override
    public DensitySource deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        Identifier id = Identifier.parse(p.getString());

        try (InputStream in = StandardGeneratorUtils.read("density", id)) {
            return StandardGeneratorUtils.decode(in, DensitySource.class);
        } catch (IOException e) {
            throw new RuntimeException("While decoding density source " + id, e);
        }
    }
}
