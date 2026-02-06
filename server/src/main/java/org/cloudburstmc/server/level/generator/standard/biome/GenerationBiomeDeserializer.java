package org.cloudburstmc.server.level.generator.standard.biome;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.store.StandardGeneratorStores;

/**
 * @author DaPorkchop_
 */
final class GenerationBiomeDeserializer extends ValueDeserializer<GenerationBiome> {
    @Override
    public GenerationBiome deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        return StandardGeneratorStores.generationBiome().find(ctxt.readValue(p, Identifier.class));
    }
}
