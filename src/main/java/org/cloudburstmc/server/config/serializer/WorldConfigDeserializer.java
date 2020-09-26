package org.cloudburstmc.server.config.serializer;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.cloudburstmc.server.config.WorldConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WorldConfigDeserializer extends StdDeserializer<Map<String, WorldConfig>> {

    public WorldConfigDeserializer() {
        this(null);
    }

    public WorldConfigDeserializer(Class<Map<String, WorldConfig>> t) {
        super(t);
    }

    @Override
    public Map<String, WorldConfig> deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        Map<String, WorldConfig> result = new HashMap<>();
        Map<String, WorldConfig> parsed = parser.getCodec().readValue(parser, new TypeReference<Map<String, WorldConfig>>() {});
        parsed.forEach((k,v)->{
            result.put(
                    k,
                    WorldConfig.builder()
                            //current behavior: use world name as seed when not specified
                            .seed(v.getSeed() == null ? k : v.getSeed())
                            .generator(v.getGenerator())
                            .options(v.getOptions())
                            .build()
            );
        });
        return result;
    }

}
