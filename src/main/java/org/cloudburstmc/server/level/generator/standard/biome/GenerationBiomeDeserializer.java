package org.cloudburstmc.server.level.generator.standard.biome;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.cloudburstmc.server.Nukkit;
import org.cloudburstmc.server.level.generator.standard.store.StandardGeneratorStores;
import org.cloudburstmc.server.utils.Identifier;

import java.io.IOException;

/**
 * @author DaPorkchop_
 */
final class GenerationBiomeDeserializer extends JsonDeserializer<GenerationBiome> {
    @Override
    public GenerationBiome deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return StandardGeneratorStores.generationBiome().find(Nukkit.YAML_MAPPER.readValue(p, Identifier.class));
    }
}
