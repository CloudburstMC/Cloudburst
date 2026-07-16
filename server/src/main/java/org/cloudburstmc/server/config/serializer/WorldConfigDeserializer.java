package org.cloudburstmc.server.config.serializer;


import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import org.cloudburstmc.server.config.ServerConfig;

import java.util.HashMap;
import java.util.Map;

public class WorldConfigDeserializer extends StdDeserializer<Map<String, ServerConfig.World>> {

    @SuppressWarnings("unchecked")
    public WorldConfigDeserializer() {
        super((Class<Map<String, ServerConfig.World>>) (Class<?>) Map.class);
    }

    @Override
    public Map<String, ServerConfig.World> deserialize(JsonParser parser, DeserializationContext deserializationContext) throws JacksonException {
        Map<String, ServerConfig.World> result = new HashMap<>();
        Map<String, ServerConfig.World> parsed = parser.readValueAs(new TypeReference<Map<String, ServerConfig.World>>() {});
        parsed.forEach((k,v)->{
            result.put(
                    k,
                    ServerConfig.World.builder()
                            //current behavior: use world name as seed when not specified
                            .seed(v.getSeed() == null ? k : v.getSeed())
                            .generator(v.getGenerator())
                            .options(v.getOptions())
                            .maxLiquidTicks(v.getMaxLiquidTicks())
                            .waterOverLavaFlowSpeed(v.getWaterOverLavaFlowSpeed())
                            .build()
            );
        });
        return result;
    }

}
