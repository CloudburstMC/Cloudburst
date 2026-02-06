package org.cloudburstmc.server.level.generator.standard.generation.decorator;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import com.google.common.base.Preconditions;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.misc.NextGenerationPass;
import org.cloudburstmc.server.level.generator.standard.registry.StandardGeneratorRegistries;

/**
 * @author DaPorkchop_
 */
final class DecoratorDeserializer extends ValueDeserializer<Decorator> {
    @Override
    public Decorator deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String nextName = p.nextName();
        Preconditions.checkState("id".equals(nextName), "first field must be \"id\", not \"%s\"", nextName);
        Identifier id = Identifier.parse(p.nextStringValue());
        p.nextToken();

        try {
            return id == NextGenerationPass.ID
                    ? NextGenerationPass.INSTANCE : ctxt.readValue(p, StandardGeneratorRegistries.decorator().get(id));
        } catch (Exception e) {
            throw new RuntimeException("While decoding decorator " + id, e);
        }
    }
}
