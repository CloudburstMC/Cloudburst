package org.cloudburstmc.server.level.generator.standard.generation.noise;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import com.google.common.base.Preconditions;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.registry.StandardGeneratorRegistries;

/**
 * @author DaPorkchop_
 */
final class NoiseGeneratorDeserializer extends ValueDeserializer<NoiseGenerator> {
    @Override
    public NoiseGenerator deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String nextName = p.nextName();
        Preconditions.checkState("id".equals(nextName), "first field must be \"id\", not \"%s\"", nextName);
        Identifier id = Identifier.parse(p.nextStringValue());
        p.nextToken();

        try {
            return ctxt.readValue(p, StandardGeneratorRegistries.noiseGenerator().get(id));
        } catch (Exception e) {
            throw new RuntimeException("While decoding noise generator " + id, e);
        }
    }
}
