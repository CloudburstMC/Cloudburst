package org.cloudburstmc.server.level.generator.standard.biome;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.level.generator.standard.store.StandardGeneratorStores;

import java.io.IOException;

/**
 * @author DaPorkchop_
 */
final class GenerationBiomeDeserializer extends JsonDeserializer<GenerationBiome> {
    @Override
    public GenerationBiome deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return StandardGeneratorStores.generationBiome().find(Bootstrap.YAML_MAPPER.readValue(p, Identifier.class));
    }
}
