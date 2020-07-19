package org.cloudburstmc.server.level.generator.standard.biome.map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.base.Preconditions;
import org.cloudburstmc.server.Nukkit;
import org.cloudburstmc.server.level.generator.standard.registry.StandardGeneratorRegistries;
import org.cloudburstmc.server.utils.Identifier;

import java.io.IOException;

/**
 * @author DaPorkchop_
 */
final class BiomeMapDeserializer extends JsonDeserializer<BiomeMap> {
    @Override
    public BiomeMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String nextName = p.nextFieldName();
        Preconditions.checkState("id".equals(nextName), "first field must be \"id\", not \"%s\"", nextName);
        Identifier id = Identifier.fromString(p.nextTextValue());
        p.nextToken();

        return Nukkit.YAML_MAPPER.readValue(p, StandardGeneratorRegistries.biomeMap().get(id));
    }
}
