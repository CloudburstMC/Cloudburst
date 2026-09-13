package org.cloudburstmc.server.config.serializer;

import org.cloudburstmc.server.config.ServerConfig;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.HashMap;
import java.util.Map;

public class WorldConfigDeserializer extends StdDeserializer<Map<String, ServerConfig.World>> {

    @SuppressWarnings("unchecked")
    public WorldConfigDeserializer() {
        super(Map.class);
    }

    @Override
    public Map<String, ServerConfig.World> deserialize(JsonParser parser, DeserializationContext deserializationContext) throws JacksonException {
        Map<String, ServerConfig.World> result = new HashMap<>();
        Map<String, ServerConfig.World> parsed = parser.readValueAs(new TypeReference<>() {});
        parsed.forEach((name, world) -> result.put(
                name,
                ServerConfig.World.builder()
                        .dimension(world.getDimension())
                        .seed(world.getSeed() == null ? name : world.getSeed())
                        .generator(world.getGenerator())
                        .options(world.getOptions())
                        .maxLiquidTicks(world.getMaxLiquidTicks())
                        .waterOverLavaFlowSpeed(world.getWaterOverLavaFlowSpeed())
                        .build()
        ));
        return result;
    }
}
