package org.cloudburstmc.server.level.generator.standard.population;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.base.Preconditions;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.level.generator.standard.misc.NextGenerationPass;
import org.cloudburstmc.server.level.generator.standard.registry.StandardGeneratorRegistries;

import java.io.IOException;

/**
 * @author DaPorkchop_
 */
final class PopulatorDeserializer extends JsonDeserializer<Populator> {
    @Override
    public Populator deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String nextName = p.nextFieldName();
        Preconditions.checkState("id".equals(nextName), "first field must be \"id\", not \"%s\"", nextName);
        Identifier id = Identifier.parse(p.nextTextValue());
        p.nextToken();

        try {
            return id == NextGenerationPass.ID
                    ? NextGenerationPass.INSTANCE : Bootstrap.YAML_MAPPER.readValue(p, StandardGeneratorRegistries.populator().get(id));
        } catch (Exception e) {
            throw new RuntimeException("While decoding populator " + id, e);
        }
    }
}
