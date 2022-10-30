package org.cloudburstmc.server.config.serializer;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.cloudburstmc.server.config.ServerConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WorldConfigDeserializer extends StdDeserializer<Map<String, ServerConfig.World>> {

    public WorldConfigDeserializer() {
        this(null);
    }

    public WorldConfigDeserializer(Class<Map<String, ServerConfig.World>> t) {
        super(t);
    }

    @Override
    public Map<String, ServerConfig.World> deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        Map<String, ServerConfig.World> result = new HashMap<>();
        Map<String, ServerConfig.World> parsed = parser.getCodec().readValue(parser, new TypeReference<Map<String, ServerConfig.World>>() {});
        parsed.forEach((k,v)->{
            result.put(
                    k,
                    ServerConfig.World.builder()
                            //Leave seed as null to be auto-generated later if the seed is not specified
                            .seed(v.getSeed())
                            .generator(v.getGenerator())
                            .options(v.getOptions())
                            .build()
            );
        });
        return result;
    }

}
